package slimeknights.mantle.data.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Predicate registry that also accepts any type of another registry, applied to a value taken from the input.
 * For example a living entity predicate accepting entity predicates; such fallbacks are written without nesting.
 * @param <T>  Input type
 * @param <F>  Fallback registry's input type
 */
public class FallbackPredicateRegistry<T, F> extends PredicateRegistry<T> {
  private final Function<T, F> getter;
  private final PredicateRegistry<F> fallback;
  private final RecordLoadable<FallbackPredicate> fallbackLoader;

  /**
   * Creates a registry.
   * @param fallback      Registry used for types this one does not have
   * @param getter        Gets the fallback input from this input
   * @param fallbackName  Name of the explicit fallback type, also the prefix of its {@code _type} key
   */
  public FallbackPredicateRegistry(String name, IJsonPredicate<T> anyInstance, @Nullable IJsonPredicate<T> noneInstance,
                                   PredicateRegistry<F> fallback, Function<T, F> getter, String fallbackName) {
    super(name, anyInstance, noneInstance);
    this.fallback = fallback;
    this.getter = getter;
    this.fallbackLoader = RecordLoadable.create(fallback.directField(fallbackName + "_type", p -> p.predicate), FallbackPredicate::new);
    register(Mantle.getResource(fallbackName), fallbackLoader);
  }

  /** Creates a predicate applying the fallback predicate to the fallback input */
  public IJsonPredicate<T> fallback(IJsonPredicate<F> predicate) {
    return new FallbackPredicate(predicate);
  }

  @Override
  public IJsonPredicate<T> convert(JsonElement element, String key, TypedMap context) {
    if (element.isJsonNull()) {
      return getDefault();
    }
    if (element.isJsonObject()) {
      return deserialize(element.getAsJsonObject(), context);
    }
    if (!compact || !element.isJsonPrimitive()) {
      throw new JsonSyntaxException("Invalid " + getName() + " JSON at " + key + ", must be a JSON object" + (compact ? " or a string" : ""));
    }
    Identifier type = JsonHelper.convertToIdentifier(element, "type");
    RecordLoadable<? extends IJsonPredicate<T>> loader = loaders.getValue(type);
    if (loader != null) {
      return loader.deserialize(new JsonObject(), context);
    }
    return new FallbackPredicate(fallback.convert(element, key, context));
  }

  @Override
  public IJsonPredicate<T> deserialize(JsonObject json, TypedMap context) {
    Identifier type = JsonHelper.getIdentifier(json, "type");
    RecordLoadable<? extends IJsonPredicate<T>> loader = loaders.getValue(type);
    if (loader != null) {
      return loader.deserialize(json, context);
    }
    return new FallbackPredicate(fallback.deserialize(json, context));
  }

  /** WHY unwrap: fallbacks are written as the fallback's own JSON, which reads back through the fallback path */
  @SuppressWarnings("unchecked")  // only FallbackPredicate implements NestedPredicate, with this registry's F
  @Override
  public JsonElement serialize(IJsonPredicate<T> src) {
    if (src instanceof NestedPredicate<?> nested) {
      return fallback.serialize(((NestedPredicate<F>) nested).predicate());
    }
    return super.serialize(src);
  }

  @SuppressWarnings("unchecked")  // only FallbackPredicate implements NestedPredicate, with this registry's F
  @Override
  public void serialize(IJsonPredicate<T> src, JsonObject json) {
    if (src instanceof NestedPredicate<?> nested) {
      fallback.serialize(((NestedPredicate<F>) nested).predicate(), json);
    } else {
      super.serialize(src, json);
    }
  }

  /** Predicate wrapping a fallback predicate */
  private interface NestedPredicate<F> {
    IJsonPredicate<F> predicate();
  }

  /** Predicate applying a fallback predicate to the fallback input */
  @RequiredArgsConstructor
  public class FallbackPredicate implements IJsonPredicate<T>, NestedPredicate<F> {
    private final IJsonPredicate<F> predicate;

    @Override
    public IJsonPredicate<F> predicate() {
      return predicate;
    }

    @Override
    public boolean matches(T input) {
      return predicate.matches(getter.apply(input));
    }

    @Override
    public IJsonPredicate<T> inverted() {
      return invert(this);
    }

    @Override
    public RecordLoadable<? extends IJsonPredicate<T>> getLoader() {
      return fallbackLoader;
    }
  }
}
