package slimeknights.mantle.data.datamap;

import com.google.common.base.Splitter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Builds and matches the block state variant keys of {@link BlockStateDataMapLoader}, such as {@code facing=north,lit=true} */
public class StateVariantStringBuilder {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',');
  private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

  private final Block owner;
  private final Collection<Property<?>> properties;
  /** Chosen values, sorted by property name so the same choices always give the same key */
  private final SortedMap<Property<?>, Comparable<?>> setStates = new TreeMap<>(Comparator.comparing(Property::getName));

  public StateVariantStringBuilder(Block owner) {
    this.owner = owner;
    this.properties = owner.getStateDefinition().getProperties();
  }

  /**
   * Requires a property to have a value.
   * @throws IllegalArgumentException  If the block lacks the property or it was already set
   */
  public <T extends Comparable<T>> StateVariantStringBuilder when(Property<T> prop, T value) {
    if (!properties.contains(prop)) {
      throw new IllegalArgumentException("Property " + prop + " is not valid for " + BuiltInRegistries.BLOCK.getKey(owner));
    }
    Comparable<?> oldValue = setStates.putIfAbsent(prop, value);
    if (oldValue != null) {
      throw new IllegalArgumentException("Property " + prop + " has already been set");
    }
    return this;
  }

  /** Builds the variant key; empty when no property is set, which matches every state */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<Property<?>, Comparable<?>> entry : setStates.entrySet()) {
      if (!builder.isEmpty()) {
        builder.append(',');
      }
      builder.append(entry.getKey().getName()).append('=').append(valueName(entry.getKey(), entry.getValue()));
    }
    return builder.toString();
  }

  /** Gets the name of a property value; {@link #when} guarantees the value has the property's type */
  @SuppressWarnings("unchecked")
  private static <T extends Comparable<T>> String valueName(Property<T> property, Comparable<?> value) {
    return property.getName((T) value);
  }

  /**
   * Makes a predicate matching the states a variant key describes.
   * Same rules as the variant keys of vanilla block state files, usable on the server too.
   * @param container  State definition of the block
   * @param variant    Variant key; properties it leaves out match any value
   * @throws IllegalArgumentException  If the key names an unknown property or value
   */
  public static Predicate<BlockState> predicate(StateDefinition<Block, BlockState> container, String variant) {
    Map<Property<?>, Comparable<?>> required = new HashMap<>();
    for (String propString : COMMA_SPLITTER.split(variant)) {
      Iterator<String> iterator = EQUAL_SPLITTER.split(propString).iterator();
      String propName = iterator.next();
      Property<?> property = container.getProperty(propName);
      if (property != null && iterator.hasNext()) {
        String valueString = iterator.next();
        Comparable<?> value = property.getValue(valueString).orElseThrow(() -> new IllegalArgumentException(
          "Unknown value: '" + valueString + "' for blockstate property: '" + propName + "' " + property.getPossibleValues()));
        required.put(property, value);
      } else if (!propName.isEmpty()) {
        throw new IllegalArgumentException("Unknown blockstate property: '" + propName + "'");
      }
    }

    Block block = container.getOwner();
    return state -> {
      if (!state.is(block)) {
        return false;
      }
      for (Map.Entry<Property<?>, Comparable<?>> entry : required.entrySet()) {
        if (!Objects.equals(state.getValue(entry.getKey()), entry.getValue())) {
          return false;
        }
      }
      return true;
    };
  }
}
