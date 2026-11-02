package slimeknights.mantle.util.typed;

import java.util.function.Supplier;

/** Typed map that can be modified */
public interface MutableTypedMap extends TypedMap {
  /** Stores the value under the key */
  <K> void put(Key<K> key, K value);

  /** Removes the value stored under the key */
  void remove(Key<?> key);

  /** Removes every entry */
  void clear();

  /** Gets the value for the key, creating and storing it with the key when missing */
  default <K> K computeIfAbsent(ComputingKey<K> key) {
    K value = get(key);
    if (value == null) {
      value = key.get();
      put(key, value);
    }
    return value;
  }

  /** Key that can create its own value when missing */
  interface ComputingKey<K> extends Key<K>, Supplier<K> {}
}
