package slimeknights.mantle.data.loadable.mapping;

import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loadable for a collection whose concrete type is chosen by the builder function, mainly to back maps */
public class AnyCollectionLoadable<T> extends CollectionLoadable<T, Collection<T>> {
  private final Function<Collection<T>, Collection<T>> builder;

  /** @param builder  Turns the read elements into the final collection */
  public AnyCollectionLoadable(Loadable<T> base, int minSize, Function<Collection<T>, Collection<T>> builder) {
    super(base, minSize);
    this.builder = builder;
  }

  /** Creates a loadable that builds an immutable list */
  public static <T> AnyCollectionLoadable<T> listBacked(Loadable<T> base, int minSize) {
    return new AnyCollectionLoadable<>(base, minSize, List::copyOf);
  }

  /** Creates a loadable that builds an immutable set */
  public static <T> AnyCollectionLoadable<T> setBacked(Loadable<T> base, int minSize) {
    return new AnyCollectionLoadable<>(base, minSize, Set::copyOf);
  }

  @Override
  protected Collection<T> build(Collection<T> builder) {
    return this.builder.apply(builder);
  }

  /** Makes a map stored as a list of values, with each key taken from its value */
  public <K> Loadable<Map<K, T>> mapWithKeys(Function<T, K> keyGetter) {
    return map(collection -> collection.stream().collect(Collectors.toUnmodifiableMap(keyGetter, Function.identity())), Map::values);
  }

  /** Makes a map stored as a list of keys, with each value computed from its key */
  public <V> Loadable<Map<T, V>> mapWithValues(Function<T, V> valueGetter) {
    return map(collection -> collection.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), valueGetter)), Map::keySet);
  }

  @Override
  public <P> LoadableField<Collection<T>, P> emptyField(String key, boolean serializeEmpty, Function<P, Collection<T>> getter) {
    return defaultField(key, List.of(), serializeEmpty, getter);
  }
}
