package slimeknights.mantle.data.predicate;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

/**
 * Predicate loaded from JSON through a {@link PredicateRegistry}.
 * @param <I>  Input type
 */
public interface IJsonPredicate<I> extends IHaveLoader {
  /** Checks if the input matches */
  boolean matches(I input);

  /** Gets a predicate matching the opposite inputs */
  IJsonPredicate<I> inverted();

  @Override
  RecordLoadable<? extends IJsonPredicate<I>> getLoader();
}
