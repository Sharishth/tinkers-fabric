package slimeknights.mantle.util.typed;

import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Typed map stored in a plain map.
 * The caller makes sure every value matches its key's type. Mutating methods throw when the backing map is immutable,
 * so hand out an immutable one only as a {@link TypedMap}.
 * @param map  Backing map
 */
@SuppressWarnings("unchecked")  // values are stored per key type, so reading them back as that type is safe
public record BackedTypedMap(Map<Key<?>, Object> map) implements MutableTypedMap {
  /** Creates an empty mutable map; keys are compared by identity, as keys are usually singletons */
  public BackedTypedMap() {
    this(new IdentityHashMap<>());
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Key<?> key) {
    return map.containsKey(key);
  }

  @Override
  public <K> @Nullable K get(Key<K> key) {
    return (K) map.get(key);
  }

  @Override
  public <R, K extends R> @Nullable R getOrDefault(Key<K> key, @Nullable R defaultValue) {
    Object value = map.get(key);
    return value != null ? (R) value : defaultValue;
  }

  @Override
  public Set<Key<?>> keySet() {
    return map.keySet();
  }

  @Override
  public <K> void put(Key<K> key, K value) {
    map.put(key, value);
  }

  @Override
  public void remove(Key<?> key) {
    map.remove(key);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public <K> K computeIfAbsent(ComputingKey<K> key) {
    return (K) map.computeIfAbsent(key, k -> key.get());
  }
}
