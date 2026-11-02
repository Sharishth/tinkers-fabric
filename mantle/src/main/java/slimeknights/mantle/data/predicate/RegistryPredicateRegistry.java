package slimeknights.mantle.data.predicate;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Predicate registry for registry entries, adding a {@code mantle:set} type for a list of exact entries.
 * @param <R>  Registry entry type
 * @param <T>  Input type
 */
public class RegistryPredicateRegistry<R, T> extends TagPredicateRegistry<R, T> {
  private final Function<T, R> getter;
  private final RecordLoadable<SetPredicate> setLoader;

  /**
   * Creates a registry.
   * @param registry  Loadable for registry entries
   * @param getter    Gets the registry entry of an input
   * @param setKey    JSON key of the entry list in the set type, such as "blocks"
   */
  public RegistryPredicateRegistry(String name, IJsonPredicate<T> anyInstance, @Nullable IJsonPredicate<T> noneInstance, Loadable<R> registry,
                                   Function<T, R> getter, String setKey, Loadable<TagKey<R>> tagKey, BiPredicate<TagKey<R>, T> tagMatcher) {
    super(name, anyInstance, noneInstance, tagKey, tagMatcher);
    this.getter = getter;
    this.setLoader = RecordLoadable.create(registry.set().requiredField(setKey, p -> p.set), SetPredicate::new);
    register(Mantle.getResource("set"), setLoader);
  }

  /** Creates a predicate matching any of the entries */
  public IJsonPredicate<T> setOf(Set<R> values) {
    return new SetPredicate(values);
  }

  /** Creates a predicate matching any of the entries */
  @SafeVarargs
  public final IJsonPredicate<T> setOf(R... values) {
    return setOf(ImmutableSet.copyOf(values));
  }

  /** Predicate matching inputs whose entry is in a set */
  @RequiredArgsConstructor
  private class SetPredicate implements IJsonPredicate<T> {
    private final Set<R> set;

    @Override
    public boolean matches(T input) {
      return set.contains(getter.apply(input));
    }

    @Override
    public IJsonPredicate<T> inverted() {
      return invert(this);
    }

    @Override
    public RecordLoadable<? extends IJsonPredicate<T>> getLoader() {
      return setLoader;
    }
  }
}
