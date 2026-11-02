package slimeknights.mantle.util.typed;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Read-only map whose keys carry the type of their value.
 * Loadables use it to pass parse context, such as a recipe id, down to nested fields; see {@link MutableTypedMap} to modify one.
 */
public interface TypedMap {
  /** Map with no entries */
  TypedMap EMPTY = new TypedMap() {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean containsKey(Key<?> key) {
      return false;
    }

    // WHY return the default: upstream returned null here, breaking the "non-null default gives non-null" contract
    @Override
    public <R, K extends R> @Nullable R getOrDefault(Key<K> key, @Nullable R defaultValue) {
      return defaultValue;
    }

    @Override
    public Set<Key<?>> keySet() {
      return Set.of();
    }
  };

  /** Gets the number of entries */
  int size();

  /** Checks if the map has no entries */
  boolean isEmpty();

  /** Checks if the map has a value for the key */
  boolean containsKey(Key<?> key);

  /**
   * Gets the value for the key, or the default when missing.
   * @return  The value, or {@code defaultValue}; never null when the default is not null
   */
  <R, K extends R> @Nullable R getOrDefault(Key<K> key, @Nullable R defaultValue);

  /** Gets the value for the key, or null when missing */
  default <K> @Nullable K get(Key<K> key) {
    return getOrDefault(key, null);
  }

  /** Gets every key in the map */
  Set<Key<?>> keySet();

  /** Gets the empty map */
  static TypedMap empty() {
    return EMPTY;
  }

  /**
   * Key of a typed map; usually a singleton compared by identity.
   * @param <K>  Type of the value stored under this key
   */
  @SuppressWarnings("unused")  // K only exists so the map methods can check value types
  interface Key<K> {}
}
