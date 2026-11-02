package slimeknights.mantle.data.loadable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.array.ObjectArrayLoadable;
import slimeknights.mantle.data.loadable.field.DefaultingField;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.field.NullableField;
import slimeknights.mantle.data.loadable.field.RequiredField;
import slimeknights.mantle.data.loadable.field.TryDirectField;
import slimeknights.mantle.data.loadable.mapping.AnyCollectionLoadable;
import slimeknights.mantle.data.loadable.mapping.ListLoadable;
import slimeknights.mantle.data.loadable.mapping.MappedLoadable;
import slimeknights.mantle.data.loadable.mapping.SetLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Reads and writes a value as JSON and on the network from one definition.
 * Implementations are usually built from the factories in this package rather than written by hand.
 * The network half is a vanilla {@link net.minecraft.network.codec.StreamCodec} (D-20).
 * @param <T>  Value type
 */
public interface Loadable<T> extends JsonDeserializer<T>, JsonSerializer<T>, Streamable<T> {
  /**
   * Reads a value from a JSON element of any type.
   * @param element  Element to read
   * @param key      Key the element was stored under, for error messages
   * @param context  Parse context, notably the recipe id for recipe serializers
   * @return  Read value
   * @throws JsonSyntaxException  If the element is invalid
   */
  T convert(JsonElement element, String key, TypedMap context);

  /** Reads a value with empty context */
  default T convert(JsonElement element, String key) {
    return convert(element, key, TypedMap.EMPTY);
  }

  /**
   * Writes a value to JSON.
   * @throws RuntimeException  If the value cannot be written
   */
  JsonElement serialize(T object);

  /** Gson hook; use {@link #convert(JsonElement, String, TypedMap)} instead */
  @Override
  default T deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
    return convert(json, type.getTypeName(), TypedMap.EMPTY);
  }

  /** Gson hook; use {@link #serialize(Object)} instead */
  @Override
  default JsonElement serialize(T object, Type type, JsonSerializationContext context) {
    return serialize(object);
  }


  /* Reading fields */

  /**
   * Reads a required field of a JSON object.
   * Do not override: missing values are handled by the record field, see {@link #defaultField(String, Object, Function)}.
   * @throws JsonSyntaxException  If the field is missing or invalid
   */
  default T getIfPresent(JsonObject parent, String key, TypedMap context) {
    if (parent.has(key)) {
      return convert(parent.get(key), key, context);
    }
    throw new JsonSyntaxException("Missing JSON field '" + key + "'");
  }

  /** Reads a required field with empty context */
  default T getIfPresent(JsonObject parent, String key) {
    return getIfPresent(parent, key, TypedMap.EMPTY);
  }

  /**
   * Reads a field of a JSON object, or returns the default when it is missing or JSON null.
   * Do not override, for the same reason as {@link #getIfPresent(JsonObject, String, TypedMap)}.
   * @return  Read value, or {@code defaultValue}; never null when the default is not null
   * @throws JsonSyntaxException  If the field is invalid
   */
  default @Nullable T getOrDefault(JsonObject parent, String key, @Nullable T defaultValue, TypedMap context) {
    JsonElement element = parent.get(key);
    if (element != null && !element.isJsonNull()) {
      return convert(element, key, context);
    }
    return defaultValue;
  }

  /** Reads a field with a default and empty context */
  default @Nullable T getOrDefault(JsonObject parent, String key, @Nullable T defaultValue) {
    return getOrDefault(parent, key, defaultValue, TypedMap.EMPTY);
  }


  /* Record fields */

  /** Creates a field that must be present */
  default <P> LoadableField<T, P> requiredField(String key, Function<P, T> getter) {
    return new RequiredField<>(this, key, false, getter);
  }

  /** Creates a field that is null when missing */
  default <P> LoadableField<T, P> nullableField(String key, Function<P, @Nullable T> getter) {
    return new NullableField<>(this, key, getter);
  }

  /**
   * Creates a field that uses a default value when missing.
   * @param serializeDefault  If false, the default value is left out of written JSON
   */
  default <P> LoadableField<T, P> defaultField(String key, T defaultValue, boolean serializeDefault, Function<P, T> getter) {
    return new DefaultingField<>(this, key, defaultValue, serializeDefault, getter);
  }

  /** Creates a field that uses a default value when missing, and leaves it out of written JSON */
  default <P> LoadableField<T, P> defaultField(String key, T defaultValue, Function<P, T> getter) {
    return defaultField(key, defaultValue, false, getter);
  }

  /**
   * Creates a field that is written straight into the parent object when that is unambiguous, otherwise under the key.
   * @param conflicts  Extra parent keys that would make writing directly ambiguous, such as optional sibling fields
   */
  default <P> LoadableField<T, P> tryDirectField(String key, Function<P, T> getter, String... conflicts) {
    return new TryDirectField<>(this, key, getter, conflicts);
  }


  /* Collections */

  /**
   * Makes an array of this loadable.
   * @param constructor  Creates an array of the given size
   * @param allowNull    If true, elements may be null
   * @param minSize      Fewest elements allowed; {@link ArrayLoadable#COMPACT} also accepts a single unwrapped element
   * @param maxSize      Most elements allowed
   */
  default ArrayLoadable<T[]> array(IntFunction<T[]> constructor, boolean allowNull, int minSize, int maxSize) {
    return new ObjectArrayLoadable<>(this, constructor, minSize, maxSize, allowNull);
  }

  /** Makes an array of this loadable with no size limit */
  default ArrayLoadable<T[]> array(IntFunction<T[]> constructor, boolean allowNull, int minSize) {
    return array(constructor, allowNull, minSize, Integer.MAX_VALUE);
  }

  /** Makes an immutable list of this loadable; {@code minSize} as in {@link #array(IntFunction, boolean, int, int)} */
  default ListLoadable<T> list(int minSize) {
    return new ListLoadable<>(this, minSize);
  }

  /** Makes an immutable list of at least one element */
  default ListLoadable<T> list() {
    return list(1);
  }

  /** Makes an immutable set of this loadable; {@code minSize} as in {@link #array(IntFunction, boolean, int, int)} */
  default SetLoadable<T> set(int minSize) {
    return new SetLoadable<>(this, minSize);
  }

  /** Makes an immutable set of at least one element */
  default SetLoadable<T> set() {
    return set(1);
  }

  /** Makes a map stored as a list of values, with each key taken from its value */
  default <K> Loadable<Map<K, T>> mapWithKeys(int minSize, Function<T, K> keyGetter) {
    return AnyCollectionLoadable.setBacked(this, minSize).mapWithKeys(keyGetter);
  }

  /** Makes a map stored as a list of keys, with each value computed from its key */
  default <V> Loadable<Map<T, V>> mapWithValues(int minSize, Function<T, V> valueGetter) {
    return AnyCollectionLoadable.setBacked(this, minSize).mapWithValues(valueGetter);
  }


  /* Mapping */

  /** Wraps a function that cannot fail as a mapping function */
  static <T, R> BiFunction<T, ErrorFactory, R> noErrors(Function<? super T, ? extends R> function) {
    return (value, error) -> function.apply(value);
  }

  /**
   * Maps this loadable to another type, where either direction may fail.
   * Throw with the given {@link ErrorFactory} so failures get the right exception type for JSON or the network.
   * @param constructor  Converts this type to the new type, typically a constructor
   * @param getter       Converts the new type back, typically a getter
   * @param <M>  New type
   */
  default <M> Loadable<M> mapChecked(BiFunction<T, ErrorFactory, M> constructor, BiFunction<M, ErrorFactory, T> getter) {
    return MappedLoadable.of(this, constructor, getter);
  }

  /**
   * Maps this loadable to another type, assuming the mapping cannot fail.
   * WHY wildcard parameters: the method overrides {@link net.minecraft.network.codec.StreamCodec#map}, which every loadable inherits.
   * Use {@link #mapChecked(BiFunction, BiFunction)} when it can fail.
   * @param <M>  New type
   */
  @Override
  default <M> Loadable<M> map(Function<? super T, ? extends M> constructor, Function<? super M, ? extends T> getter) {
    return mapChecked(noErrors(constructor), noErrors(getter));
  }

  /** Checks each value with the validator, which returns the value when valid and throws otherwise */
  default Loadable<T> validate(BiFunction<T, ErrorFactory, T> validator) {
    return mapChecked(validator, validator);
  }
}
