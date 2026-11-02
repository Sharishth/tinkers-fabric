package slimeknights.mantle.data.predicate.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Predicate matching one block with some of its properties set, as values, value lists or ranges.
 * JSON: {@code {"block": id, "properties": {"age": "3", "facing": ["north", "south"], "level": {"min": "2"}}}}.
 * @param block       Block to match
 * @param properties  Property matchers, all of which must match
 */
public record BlockPropertiesPredicate(Block block, List<Matcher> properties) implements BlockPredicate {
  private static final Function<String, RuntimeException> JSON_EXCEPTION = JsonSyntaxException::new;
  private static final Function<String, RuntimeException> DECODER_EXCEPTION = DecoderException::new;
  /** Longest property name or value read from the network */
  private static final int MAX_NAME_LENGTH = Short.MAX_VALUE;

  @Override
  public boolean matches(BlockState input) {
    if (input.getBlock() != block) {
      return false;
    }
    for (Matcher matcher : properties) {
      if (!matcher.matches(input)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public RecordLoadable<BlockPropertiesPredicate> getLoader() {
    return LOADER;
  }

  /** Gets a property of the block by name, or throws the given exception type */
  private static Property<?> parseProperty(Block block, String name, Function<String, RuntimeException> exception) {
    Property<?> property = block.getStateDefinition().getProperty(name);
    if (property == null) {
      throw exception.apply("Property " + name + " does not exist in block " + BuiltInRegistries.BLOCK.getKey(block));
    }
    return property;
  }

  /** Gets a property value by name, or throws the given exception type */
  private static <T extends Comparable<T>> T parseValue(Property<T> property, String name, Function<String, RuntimeException> exception) {
    Optional<T> value = property.getValue(name);
    if (value.isEmpty()) {
      throw exception.apply("Unknown property value " + name + " for " + property.getName());
    }
    return value.get();
  }

  public static final RecordLoadable<BlockPropertiesPredicate> LOADER = new RecordLoadable<>() {
    @Override
    public BlockPropertiesPredicate deserialize(JsonObject json, TypedMap context) {
      Block block = Loadables.BLOCK.getIfPresent(json, "block", context);
      Set<Entry<String, JsonElement>> properties = GsonHelper.getAsJsonObject(json, "properties").entrySet();
      List<Matcher> matchers = new ArrayList<>(properties.size());
      for (Entry<String, JsonElement> entry : properties) {
        matchers.add(Matcher.deserialize(parseProperty(block, entry.getKey(), JSON_EXCEPTION), entry.getValue()));
      }
      return new BlockPropertiesPredicate(block, List.copyOf(matchers));
    }

    @Override
    public void serialize(BlockPropertiesPredicate object, JsonObject json) {
      json.add("block", Loadables.BLOCK.serialize(object.block));
      JsonObject properties = new JsonObject();
      for (Matcher matcher : object.properties) {
        properties.add(matcher.property().getName(), matcher.serialize());
      }
      json.add("properties", properties);
    }

    @Override
    public BlockPropertiesPredicate decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      Block block = Loadables.BLOCK.decode(buffer, context);
      int size = buffer.readVarInt();
      List<Matcher> matchers = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        matchers.add(Matcher.fromNetwork(block, buffer));
      }
      return new BlockPropertiesPredicate(block, List.copyOf(matchers));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, BlockPropertiesPredicate object) {
      Loadables.BLOCK.encode(buffer, object.block);
      buffer.writeVarInt(object.properties.size());
      for (Matcher matcher : object.properties) {
        matcher.toNetwork(buffer);
      }
    }
  };

  /** Matches one property of a state */
  public sealed interface Matcher permits SetMatcher, RangeMatcher {
    /** Checks if the state's value matches */
    boolean matches(BlockState state);

    /** Gets the matched property */
    Property<?> property();

    /** Writes the matcher's JSON value */
    JsonElement serialize();

    /**
     * Writes the matcher to the network, values as their names.
     * WHY names: vanilla properties have no network form, so names are the only stable encoding.
     */
    void toNetwork(RegistryFriendlyByteBuf buffer);

    /** Reads a matcher: a value name is an exact match, a list matches any, an object is a min/max range */
    static <T extends Comparable<T>> Matcher deserialize(Property<T> property, JsonElement element) {
      String name = property.getName();
      if (element.isJsonPrimitive()) {
        return new SetMatcher<>(property, parseValue(property, GsonHelper.convertToString(element, name), JSON_EXCEPTION));
      }
      if (element.isJsonArray()) {
        JsonArray array = element.getAsJsonArray();
        Set<T> values = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
          values.add(parseValue(property, GsonHelper.convertToString(array.get(i), name + '[' + i + ']'), JSON_EXCEPTION));
        }
        if (values.isEmpty()) {
          throw new JsonSyntaxException("Value list for " + name + " must not be empty");
        }
        return new SetMatcher<>(property, Set.copyOf(values));
      }
      if (element.isJsonObject()) {
        return deserializeRange(property, element.getAsJsonObject());
      }
      throw new JsonSyntaxException("Invalid matcher type " + GsonHelper.getType(element));
    }

    /** Reads a range matcher; equal bounds become an exact match */
    private static <T extends Comparable<T>> Matcher deserializeRange(Property<T> property, JsonObject json) {
      T min = json.has("min") ? parseValue(property, GsonHelper.getAsString(json, "min"), JSON_EXCEPTION) : null;
      T max = json.has("max") ? parseValue(property, GsonHelper.getAsString(json, "max"), JSON_EXCEPTION) : null;
      if (min == null && max == null) {
        throw new JsonSyntaxException("Either min or max must be set for a range matcher");
      }
      if (min != null && min.equals(max)) {
        return new SetMatcher<>(property, min);
      }
      return new RangeMatcher<>(property, min, max);
    }

    /** Reads a matcher for a property of the block */
    static Matcher fromNetwork(Block block, RegistryFriendlyByteBuf buffer) {
      return fromNetwork(parseProperty(block, buffer.readUtf(MAX_NAME_LENGTH), DECODER_EXCEPTION), buffer);
    }

    /** Reads a matcher; a size of 0 means a range, otherwise the number of set values */
    static <T extends Comparable<T>> Matcher fromNetwork(Property<T> property, RegistryFriendlyByteBuf buffer) {
      int size = buffer.readVarInt();
      if (size == 0) {
        RangeType rangeType = RangeType.read(buffer);
        T min = rangeType != RangeType.MAX ? parseValue(property, buffer.readUtf(MAX_NAME_LENGTH), DECODER_EXCEPTION) : null;
        T max = rangeType != RangeType.MIN ? parseValue(property, buffer.readUtf(MAX_NAME_LENGTH), DECODER_EXCEPTION) : null;
        return new RangeMatcher<>(property, min, max);
      }
      List<T> values = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        values.add(parseValue(property, buffer.readUtf(MAX_NAME_LENGTH), DECODER_EXCEPTION));
      }
      return new SetMatcher<>(property, Set.copyOf(values));
    }
  }

  /**
   * Matches states whose value is in a set.
   * @param property  Property to check
   * @param values    Values that match; not empty
   */
  public record SetMatcher<T extends Comparable<T>>(Property<T> property, Set<T> values) implements Matcher {
    public SetMatcher {
      if (values.isEmpty()) {
        throw new IllegalArgumentException("Values must not be empty");
      }
    }

    /** Creates a matcher for a single value */
    public SetMatcher(Property<T> property, T value) {
      this(property, Set.of(value));
    }

    @Override
    public boolean matches(BlockState state) {
      return values.contains(state.getValue(property));
    }

    @Override
    public JsonElement serialize() {
      if (values.size() == 1) {
        return new JsonPrimitive(property.getName(values.iterator().next()));
      }
      JsonArray array = new JsonArray();
      for (T value : values) {
        array.add(property.getName(value));
      }
      return array;
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buffer) {
      buffer.writeUtf(property.getName());
      buffer.writeVarInt(values.size());
      for (T value : values) {
        buffer.writeUtf(property.getName(value));
      }
    }
  }

  /** Which bounds a range sends */
  private enum RangeType {
    FULL, MIN, MAX;

    /** Gets the type for the bounds; at least one must be set */
    static RangeType fromValues(@Nullable Object min, @Nullable Object max) {
      if (min == null && max == null) {
        throw new IllegalArgumentException("Cannot have both min and max null");
      }
      if (max == null) {
        return MIN;
      }
      return min == null ? MAX : FULL;
    }

    // WHY a byte: 26.3 deprecates FriendlyByteBuf.readEnum/writeEnum
    void write(RegistryFriendlyByteBuf buffer) {
      buffer.writeByte(ordinal());
    }

    static RangeType read(RegistryFriendlyByteBuf buffer) {
      int index = buffer.readByte();
      RangeType[] values = values();
      if (index < 0 || index >= values.length) {
        throw new DecoderException("Invalid range type " + index);
      }
      return values[index];
    }
  }

  /**
   * Matches states whose value is between the bounds, using the property's value order.
   * @param property  Property to check
   * @param min       Lowest matching value, or null for no lower bound
   * @param max       Highest matching value, or null for no upper bound
   */
  public record RangeMatcher<T extends Comparable<T>>(Property<T> property, @Nullable T min, @Nullable T max) implements Matcher {
    public RangeMatcher {
      RangeType.fromValues(min, max);
    }

    @Override
    public boolean matches(BlockState state) {
      T value = state.getValue(property);
      return (min == null || value.compareTo(min) >= 0) && (max == null || value.compareTo(max) <= 0);
    }

    @Override
    public JsonElement serialize() {
      JsonObject json = new JsonObject();
      if (min != null) {
        json.addProperty("min", property.getName(min));
      }
      if (max != null) {
        json.addProperty("max", property.getName(max));
      }
      return json;
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buffer) {
      buffer.writeUtf(property.getName());
      buffer.writeVarInt(0);
      RangeType.fromValues(min, max).write(buffer);
      if (min != null) {
        buffer.writeUtf(property.getName(min));
      }
      if (max != null) {
        buffer.writeUtf(property.getName(max));
      }
    }
  }

  /** Starts building a predicate for the block */
  public static Builder block(Block block) {
    return new Builder(block);
  }

  /** Builds a block properties predicate in code, such as for datagen */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final Block block;
    private final Map<Property<?>, Matcher> matchers = new LinkedHashMap<>();

    /** Adds a matcher, checking the property belongs to the block and is not matched twice */
    private Builder matches(Matcher matcher) {
      Property<?> property = matcher.property();
      if (!block.getStateDefinition().getProperties().contains(property)) {
        throw new IllegalArgumentException("Property " + property + " does not exist in block " + block);
      }
      Matcher original = matchers.put(property, matcher);
      if (original != null) {
        throw new IllegalArgumentException("Matcher for property already exists: previous matcher " + original);
      }
      return this;
    }

    /** Matches any of the values */
    public <T extends Comparable<T>> Builder matches(Property<T> property, Set<T> values) {
      return matches(new SetMatcher<>(property, values));
    }

    /** Matches any of the values */
    @SafeVarargs
    public final <T extends Comparable<T>> Builder matches(Property<T> property, T... values) {
      return matches(property, Set.of(values));
    }

    /** Matches values between min and max */
    public <T extends Comparable<T>> Builder range(Property<T> property, T min, T max) {
      if (Objects.equals(min, max)) {
        return matches(property, min);
      }
      return matches(new RangeMatcher<>(property, min, max));
    }

    /** Matches values from min up */
    public <T extends Comparable<T>> Builder min(Property<T> property, T min) {
      return matches(new RangeMatcher<>(property, min, null));
    }

    /** Matches values up to max */
    public <T extends Comparable<T>> Builder max(Property<T> property, T max) {
      return matches(new RangeMatcher<>(property, null, max));
    }

    /** Builds the predicate; needs at least one property */
    public BlockPropertiesPredicate build() {
      if (matchers.isEmpty()) {
        throw new IllegalArgumentException("Must have at least one property");
      }
      return new BlockPropertiesPredicate(block, List.copyOf(matchers.values()));
    }
  }
}
