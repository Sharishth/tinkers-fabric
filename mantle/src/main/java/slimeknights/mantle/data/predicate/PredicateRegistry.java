package slimeknights.mantle.data.predicate;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.DefaultingLoaderRegistry;

import java.util.List;

/**
 * Loader registry for predicates, with the shared {@code mantle:any}, {@code mantle:none}, {@code mantle:inverted},
 * {@code mantle:and} and {@code mantle:or} types. JSON null or a missing field means "any".
 * @param <T>  Input type
 */
public class PredicateRegistry<T> extends DefaultingLoaderRegistry<IJsonPredicate<T>> {
  private final RecordLoadable<InvertedJsonPredicate> invertedLoader;
  private final RecordLoadable<AndJsonPredicate> andLoader;
  private final RecordLoadable<OrJsonPredicate> orLoader;
  /** Predicate matching nothing, if this registry has one */
  private final @Nullable IJsonPredicate<T> noneInstance;

  /**
   * Creates a registry.
   * @param anyInstance   Predicate matching everything; the default
   * @param noneInstance  Predicate matching nothing, or null if the type has none
   */
  public PredicateRegistry(String name, IJsonPredicate<T> anyInstance, @Nullable IJsonPredicate<T> noneInstance) {
    super(name, anyInstance, noneInstance, true);
    this.noneInstance = noneInstance;
    Loadable<List<IJsonPredicate<T>>> list = this.list(2);
    invertedLoader = RecordLoadable.create(directField("inverted_type", p -> p.predicate), InvertedJsonPredicate::new);
    andLoader = RecordLoadable.create(list.requiredField("predicates", p -> p.children), AndJsonPredicate::new);
    orLoader = RecordLoadable.create(list.requiredField("predicates", p -> p.children), OrJsonPredicate::new);
    register(Mantle.getResource("any"), anyInstance.getLoader());
    if (noneInstance != null) {
      register(Mantle.getResource("none"), noneInstance.getLoader());
    }
    register(Mantle.getResource("inverted"), invertedLoader);
    register(Mantle.getResource("and"), andLoader);
    register(Mantle.getResource("or"), orLoader);
  }

  /** Gets the predicate matching nothing, or null if this registry has none */
  public @Nullable IJsonPredicate<T> getNone() {
    return noneInstance;
  }

  /** Creates a predicate matching what the given one does not */
  public IJsonPredicate<T> invert(IJsonPredicate<T> predicate) {
    return new InvertedJsonPredicate(predicate);
  }

  /** Creates a predicate matching when all children match */
  public IJsonPredicate<T> and(List<IJsonPredicate<T>> predicates) {
    return new AndJsonPredicate(predicates);
  }

  /** Creates a predicate matching when any child matches */
  public IJsonPredicate<T> or(List<IJsonPredicate<T>> predicates) {
    return new OrJsonPredicate(predicates);
  }

  // TODO(T-44): conditional(ifTrue, ifFalse, conditions) and ConditionalPredicate (upstream Forge ICondition)

  /** Predicate matching the opposite of another */
  @RequiredArgsConstructor
  public class InvertedJsonPredicate implements IJsonPredicate<T> {
    private final IJsonPredicate<T> predicate;

    @Override
    public boolean matches(T input) {
      return !predicate.matches(input);
    }

    @Override
    public IJsonPredicate<T> inverted() {
      return predicate;
    }

    @Override
    public RecordLoadable<? extends IJsonPredicate<T>> getLoader() {
      return invertedLoader;
    }
  }

  /** Predicate matching when all children match */
  @RequiredArgsConstructor
  public class AndJsonPredicate implements IJsonPredicate<T> {
    private final List<IJsonPredicate<T>> children;

    @Override
    public boolean matches(T input) {
      for (IJsonPredicate<T> child : children) {
        if (!child.matches(input)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public IJsonPredicate<T> inverted() {
      return invert(this);
    }

    @Override
    public RecordLoadable<? extends IJsonPredicate<T>> getLoader() {
      return andLoader;
    }
  }

  /** Predicate matching when any child matches */
  @RequiredArgsConstructor
  public class OrJsonPredicate implements IJsonPredicate<T> {
    private final List<IJsonPredicate<T>> children;

    @Override
    public boolean matches(T input) {
      for (IJsonPredicate<T> child : children) {
        if (child.matches(input)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public IJsonPredicate<T> inverted() {
      return invert(this);
    }

    @Override
    public RecordLoadable<? extends IJsonPredicate<T>> getLoader() {
      return orLoader;
    }
  }
}
