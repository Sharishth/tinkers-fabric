package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Field that uses a default value when missing from JSON.
 * @param loadable       Loadable for the value
 * @param key            JSON key
 * @param defaultValue   Value used when the key is missing
 * @param skipSerialize  Given the default and the value, true leaves the value out of written JSON; null always writes it
 * @param getter         Reads the value from the parent
 */
public record DefaultingField<T, P>(Loadable<T> loadable, String key, T defaultValue, @Nullable BiPredicate<? super T, ? super T> skipSerialize,
                                    Function<P, T> getter) implements AlwaysPresentLoadableField<T, P> {
  /** Skips writing values equal to the default */
  private static final BiPredicate<Object, Object> SKIP_EQUAL = Objects::equals;

  /**
   * Creates a field that compares values to the default with {@link Object#equals(Object)}.
   * @param serializeDefault  If false, a value equal to the default is left out of written JSON
   */
  public DefaultingField(Loadable<T> loadable, String key, T defaultValue, boolean serializeDefault, Function<P, T> getter) {
    this(loadable, key, defaultValue, serializeDefault ? null : SKIP_EQUAL, getter);
  }

  /** Checks if the default value is always written */
  public boolean serializeDefault() {
    return skipSerialize == null;
  }

  @Override
  public T get(JsonObject json, String key, TypedMap context) {
    // non-null default, so the result is never null
    return Objects.requireNonNull(loadable.getOrDefault(json, key, defaultValue, context));
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    T object = getter.apply(parent);
    if (skipSerialize == null || !skipSerialize.test(defaultValue, object)) {
      json.add(key, loadable.serialize(object));
    }
  }
}
