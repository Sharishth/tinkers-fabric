package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.MapLoadable;
import slimeknights.mantle.data.loadable.mapping.MappedLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Loadable stored as a JSON string, so it can also be the key of a JSON object read as a map.
 * @param <T>  Value type
 */
@SuppressWarnings("unused")  // API
public interface StringLoadable<T> extends Loadable<T> {
  /** Plain string of up to {@link Short#MAX_VALUE} characters */
  StringLoadable<String> DEFAULT = maxLength(Short.MAX_VALUE);

  /** Creates a plain string loadable with a length limit */
  static StringLoadable<String> maxLength(int maxLength) {
    return new MaxLengthStringLoadable(maxLength);
  }

  /**
   * Reads a value from its string form.
   * @param key  JSON key, for error messages
   * @throws com.google.gson.JsonSyntaxException  If the string is invalid
   */
  T parseString(String value, String key, TypedMap context);

  /** Reads a value from its string form with empty context */
  default T parseString(String value, String key) {
    return parseString(value, key, TypedMap.EMPTY);
  }

  /**
   * Gets the string form of a value.
   * @throws RuntimeException  If the value has no string form
   */
  String getString(T object);

  @Override
  default T convert(JsonElement element, String key, TypedMap context) {
    return parseString(GsonHelper.convertToString(element, key), key, context);
  }

  @Override
  default JsonElement serialize(T object) {
    return new JsonPrimitive(getString(object));
  }


  /* Maps */

  /**
   * Makes a map stored as a JSON object, with this loadable for the keys.
   * @param valueLoadable  Loadable for the values
   * @param minSize        Fewest entries allowed
   */
  default <V> Loadable<Map<T, V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new MapLoadable<>(this, valueLoadable, minSize);
  }

  /** Makes a map of at least one entry stored as a JSON object */
  default <V> Loadable<Map<T, V>> mapWithValues(Loadable<V> valueLoadable) {
    return mapWithValues(valueLoadable, 1);
  }


  /* Mapping keeps the string form */

  @Override
  default <M> StringLoadable<M> mapChecked(BiFunction<T, ErrorFactory, M> constructor, BiFunction<M, ErrorFactory, T> getter) {
    return MappedLoadable.of(this, constructor, getter);
  }

  @Override
  default <M> StringLoadable<M> map(Function<? super T, ? extends M> constructor, Function<? super M, ? extends T> getter) {
    return mapChecked(Loadable.noErrors(constructor), Loadable.noErrors(getter));
  }

  @Override
  default StringLoadable<T> validate(BiFunction<T, ErrorFactory, T> validator) {
    return mapChecked(validator, validator);
  }
}
