package slimeknights.mantle.data.predicate;

import lombok.RequiredArgsConstructor;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.function.BiPredicate;

/**
 * Predicate registry with a {@code mantle:tag} type.
 * @param <R>  Tagged registry type
 * @param <T>  Input type
 */
public class TagPredicateRegistry<R, T> extends PredicateRegistry<T> {
  private final BiPredicate<TagKey<R>, T> tagMatcher;
  private final RecordLoadable<TagPredicate> tagLoader;

  /**
   * Creates a registry.
   * @param tagKey      Loadable for the tag key
   * @param tagMatcher  Checks if an input is in a tag
   */
  public TagPredicateRegistry(String name, IJsonPredicate<T> anyInstance, @Nullable IJsonPredicate<T> noneInstance,
                              Loadable<TagKey<R>> tagKey, BiPredicate<TagKey<R>, T> tagMatcher) {
    super(name, anyInstance, noneInstance);
    this.tagMatcher = tagMatcher;
    this.tagLoader = RecordLoadable.create(tagKey.requiredField("tag", p -> p.tag), TagPredicate::new);
    register(Mantle.getResource("tag"), tagLoader);
  }

  /** Creates a predicate matching inputs in the tag */
  public IJsonPredicate<T> tag(TagKey<R> tag) {
    return new TagPredicate(tag);
  }

  /** Predicate matching inputs in a tag */
  @RequiredArgsConstructor
  private class TagPredicate implements IJsonPredicate<T> {
    private final TagKey<R> tag;

    @Override
    public boolean matches(T input) {
      return tagMatcher.test(tag, input);
    }

    @Override
    public IJsonPredicate<T> inverted() {
      return invert(this);
    }

    @Override
    public RecordLoadable<? extends IJsonPredicate<T>> getLoader() {
      return tagLoader;
    }
  }
}
