package slimeknights.mantle.util.typed;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Builds an immutable typed map, keeping each value matched to its key's type */
@SuppressWarnings("unused")  // API
@NoArgsConstructor(staticName = "builder")
public class TypedMapBuilder {
  private final Map<TypedMap.Key<?>, Object> entries = new HashMap<>();

  /** Copies every entry of the given map */
  public TypedMapBuilder putAll(TypedMap map) {
    for (TypedMap.Key<?> key : map.keySet()) {
      entries.put(key, Objects.requireNonNull(map.get(key), "Typed map listed a key with no value"));
    }
    return this;
  }

  /** Adds a value */
  public <K> TypedMapBuilder put(TypedMap.Key<K> key, K value) {
    entries.put(key, value);
    return this;
  }

  /** Builds the map; returns {@link TypedMap#EMPTY} when nothing was added */
  public TypedMap build() {
    if (entries.isEmpty()) {
      return TypedMap.EMPTY;
    }
    return new BackedTypedMap(Map.copyOf(entries));
  }
}
