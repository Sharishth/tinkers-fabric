package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.IAmLoadable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Streamable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loadable that picks one of several loadables from the JSON shape: an array, a primitive,
 * or an object containing a given key. Values must know their own loadable ({@link IAmLoadable}),
 * which is how writing picks the right option.
 */
public final class EitherLoadable {
  private EitherLoadable() {}

  /** Creates a builder that may use arrays, primitives and keyed objects */
  public static <T extends IAmLoadable> TypedBuilder<T> typed() {
    return new TypedBuilder<>();
  }

  /** Creates a builder for keyed objects only, which builds a record loadable */
  public static <T extends IAmLoadable.Record> RecordBuilder<T> record() {
    return new RecordBuilder<>();
  }

  /** Option chosen when a JSON object has the key */
  private record KeyOption<T>(String key, RecordLoadable<? extends T> loadable) {}

  /** Builder for {@link #typed()} */
  public static class TypedBuilder<T extends IAmLoadable> {
    private final List<KeyOption<T>> keys = new ArrayList<>();
    /** Option for JSON arrays, if any */
    private @Nullable Loadable<? extends T> array = null;
    /** Option for JSON primitives, if any */
    private @Nullable Loadable<? extends T> primitive = null;

    private TypedBuilder() {}

    /** Adds an option chosen when the object has the key; earlier keys win */
    public TypedBuilder<T> key(String key, RecordLoadable<? extends T> loadable) {
      keys.add(new KeyOption<>(key, loadable));
      return this;
    }

    /** Sets the option for JSON arrays */
    public TypedBuilder<T> array(Loadable<? extends T> loadable) {
      if (this.array != null) {
        throw new IllegalStateException("Duplicate array loadable, previous value " + this.array);
      }
      this.array = loadable;
      return this;
    }

    /** Sets the option for JSON primitives */
    public TypedBuilder<T> primitive(Loadable<? extends T> loadable) {
      if (this.primitive != null) {
        throw new IllegalStateException("Duplicate primitive loadable, previous value " + this.primitive);
      }
      this.primitive = loadable;
      return this;
    }

    /** Gets the key options, checking there are at least two options in total */
    private List<KeyOption<T>> checkedKeys() {
      int size = keys.size() + (array != null ? 1 : 0) + (primitive != null ? 1 : 0);
      if (size < 2) {
        throw new IllegalStateException("EitherLoadable must have at least 2 options.");
      }
      return List.copyOf(keys);
    }

    /** Builds the loadable, sending every value on the network with the given loadable */
    public Loadable<T> build(Loadable<T> network) {
      return new Typing<>(List.of(network), checkedKeys(), array, primitive);
    }

    /** Builds the loadable; the network sends the index of the value's own option before the value */
    @SuppressWarnings("unchecked")  // each option reads and writes a subtype of T, chosen by the value's own loadable
    public Loadable<T> build() {
      List<KeyOption<T>> checked = checkedKeys();
      List<Loadable<T>> network = new ArrayList<>(checked.size() + 2);
      checked.forEach(option -> network.add((Loadable<T>) option.loadable));
      if (array != null) {
        network.add((Loadable<T>) array);
      }
      if (primitive != null) {
        network.add((Loadable<T>) primitive);
      }
      return new Typing<>(List.copyOf(network), checked, array, primitive);
    }
  }

  /** Builder for {@link #record()} */
  public static class RecordBuilder<T extends IAmLoadable.Record> {
    private final List<KeyOption<T>> keys = new ArrayList<>();

    private RecordBuilder() {}

    /** Adds an option chosen when the object has the key; earlier keys win */
    public RecordBuilder<T> key(String key, RecordLoadable<? extends T> loadable) {
      keys.add(new KeyOption<>(key, loadable));
      return this;
    }

    /** Gets the key options, checking there are at least two */
    private List<KeyOption<T>> checkedKeys() {
      if (keys.size() < 2) {
        throw new IllegalStateException("EitherLoadable must have at least 2 options.");
      }
      return List.copyOf(keys);
    }

    /** Builds the loadable, sending every value on the network with the given loadable */
    public RecordLoadable<T> build(Loadable<T> network) {
      return new RecordOnly<>(List.of(network), checkedKeys());
    }

    /** Builds the loadable; the network sends the index of the value's own option before the value */
    @SuppressWarnings("unchecked")  // each option reads and writes a subtype of T, chosen by the value's own loadable
    public RecordLoadable<T> build() {
      List<KeyOption<T>> checked = checkedKeys();
      List<Loadable<T>> network = checked.stream().<Loadable<T>>map(option -> (RecordLoadable<T>) option.loadable).toList();
      return new RecordOnly<>(network, checked);
    }
  }


  /** Logic shared by both forms */
  private interface EitherImpl<T extends IAmLoadable> extends Loadable<T> {
    /** Loadables the network may use; a single one is used for every value without an index */
    List<? extends Streamable<T>> network();

    /** Keyed object options, in priority order */
    List<KeyOption<T>> keys();

    /** Option for JSON arrays, if any */
    default @Nullable Loadable<? extends T> array() {
      return null;
    }

    /** Option for JSON primitives, if any */
    default @Nullable Loadable<? extends T> primitive() {
      return null;
    }

    /** Reads a JSON object with the first option whose key it has, or throws listing the accepted shapes */
    default T deserializeObject(JsonElement element, TypedMap context, String key) {
      if (element.isJsonObject()) {
        JsonObject json = element.getAsJsonObject();
        for (KeyOption<T> option : keys()) {
          if (json.has(option.key)) {
            return option.loadable.deserialize(json, context);
          }
        }
      }
      throw new JsonSyntaxException(shapeError(key));
    }

    /** Describes the accepted JSON shapes for an error */
    private String shapeError(String key) {
      StringBuilder builder = new StringBuilder("JSON at ").append(key).append(" must be one of: ");
      if (array() != null) {
        builder.append("array, ");
      }
      if (primitive() != null) {
        builder.append("primitive, ");
      }
      builder.append("object with key from [").append(keys().stream().map(KeyOption::key).collect(Collectors.joining(", "))).append(']');
      return builder.toString();
    }

    /** Reads which loadable wrote the value */
    default Streamable<T> loadableFromNetwork(RegistryFriendlyByteBuf buffer) {
      List<? extends Streamable<T>> networks = network();
      int size = networks.size();
      if (size == 1) {
        return networks.getFirst();
      }
      int networkIndex = buffer.readVarInt();
      if (networkIndex >= 0 && networkIndex < size) {
        return networks.get(networkIndex);
      }
      throw new DecoderException("Unknown network index " + networkIndex + " for EitherLoadable with network size " + size);
    }

    @Override
    default void encode(RegistryFriendlyByteBuf buffer, T object) {
      List<? extends Streamable<T>> networks = network();
      if (networks.size() == 1) {
        networks.getFirst().encode(buffer, object);
        return;
      }
      // reference comparison: the value's own loadable is one of the option instances
      Loadable<?> objectLoadable = object.loadable();
      for (int i = 0; i < networks.size(); i++) {
        Streamable<T> network = networks.get(i);
        if (network == objectLoadable) {
          buffer.writeVarInt(i);
          network.encode(buffer, object);
          return;
        }
      }
      throw new IllegalArgumentException("Unable to serialize " + object + " to network as its loadable " + objectLoadable + " is not allowed in the EitherLoadable");
    }
  }

  /** Form accepting arrays, primitives and keyed objects */
  private record Typing<T extends IAmLoadable>(List<Loadable<T>> network, List<KeyOption<T>> keys,
                                               @Nullable Loadable<? extends T> array, @Nullable Loadable<? extends T> primitive) implements EitherImpl<T> {
    @Override
    public T convert(JsonElement element, String key, TypedMap context) {
      if (array != null && element.isJsonArray()) {
        return array.convert(element, key, context);
      }
      if (primitive != null && element.isJsonPrimitive()) {
        return primitive.convert(element, key, context);
      }
      if (!keys.isEmpty()) {
        return deserializeObject(element, context, key);
      }
      throw new JsonSyntaxException("JSON at " + key + " must be one of: array, primitive");
    }

    @SuppressWarnings("unchecked")  // the value's own loadable accepts the value
    @Override
    public JsonElement serialize(T object) {
      return ((Loadable<T>) object.loadable()).serialize(object);
    }

    @Override
    public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return loadableFromNetwork(buffer).decode(buffer, context);
    }
  }

  /** Form accepting keyed objects only; upstream named it Record, renamed so it does not hide java.lang.Record */
  private record RecordOnly<T extends IAmLoadable.Record>(List<Loadable<T>> network, List<KeyOption<T>> keys) implements RecordLoadable<T>, EitherImpl<T> {
    @Override
    public T deserialize(JsonObject json, TypedMap context) {
      return deserializeObject(json, context, "[root]");
    }

    @SuppressWarnings("unchecked")  // the value's own loadable accepts the value
    @Override
    public void serialize(T object, JsonObject json) {
      ((RecordLoadable<T>) object.loadable()).serialize(object, json);
    }

    @Override
    public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return loadableFromNetwork(buffer).decode(buffer, context);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, T object) {
      EitherImpl.super.encode(buffer, object);
    }
  }
}
