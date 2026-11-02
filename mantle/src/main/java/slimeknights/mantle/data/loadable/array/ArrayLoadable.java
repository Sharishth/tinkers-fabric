package slimeknights.mantle.data.loadable.array;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Loadable for arrays and collections, stored as a JSON array.
 * With a compact minimum size, a single element may also be stored unwrapped.
 * @param <A>  Array or collection type
 */
public interface ArrayLoadable<A> extends Loadable<A> {
  /** Minimum size meaning "compact form allowed, may be empty" */
  int COMPACT_OR_EMPTY = -2;
  /** Minimum size meaning "compact form allowed, at least one element" */
  int COMPACT = -1;

  /** Checks if a single element may be stored unwrapped */
  boolean allowCompact();

  /**
   * Checks the number of elements.
   * @throws RuntimeException  From {@code error}, when the size is not allowed
   */
  void checkSize(String key, int size, ErrorFactory error);

  /** Gets the number of elements */
  int getLength(A array);

  /** Reads a single unwrapped element as a one-element value */
  A convertCompact(JsonElement element, String key, TypedMap context);

  /** Reads every element of a JSON array */
  A convertArray(JsonArray array, String key, TypedMap context);

  /** Writes the first element on its own */
  JsonElement serializeFirst(A object);

  /** Writes every element into the JSON array */
  void serializeAll(JsonArray array, A object);

  @Override
  default A convert(JsonElement element, String key, TypedMap context) {
    if (allowCompact() && !element.isJsonArray()) {
      return convertCompact(element, key, context);
    }
    JsonArray array = GsonHelper.convertToJsonArray(element, key);
    checkSize(key, array.size(), ErrorFactory.JSON_SYNTAX_ERROR);
    return convertArray(array, key, context);
  }

  @Override
  default JsonElement serialize(A object) {
    int length = getLength(object);
    if (allowCompact() && length == 1) {
      JsonElement element = serializeFirst(object);
      // an element that is itself an array would read back as the whole list, so it must stay wrapped
      if (!element.isJsonArray()) {
        return element;
      }
    }
    checkSize("Collection", length, ErrorFactory.RUNTIME);
    JsonArray array = new JsonArray();
    serializeAll(array, object);
    return array;
  }


  /* Fields */

  /**
   * Creates a field that is empty when missing.
   * @param serializeEmpty  If false, an empty value is left out of written JSON
   */
  <P> LoadableField<A, P> emptyField(String key, boolean serializeEmpty, Function<P, A> getter);

  /** Creates a field that is empty when missing, and left out of written JSON when empty */
  default <P> LoadableField<A, P> emptyField(String key, Function<P, A> getter) {
    return emptyField(key, false, getter);
  }

  /**
   * Array loadable limited by a minimum and maximum size.
   * @param <A>  Array type
   */
  interface SizeRange<A> extends ArrayLoadable<A> {
    /** Gets the minimum size, or {@link #COMPACT} / {@link #COMPACT_OR_EMPTY} */
    int minSize();

    /** Gets the maximum size */
    int maxSize();

    @Override
    default boolean allowCompact() {
      return minSize() < 0;
    }

    /** Gets the real smallest size, turning the compact markers into 1 and 0 */
    static int effectiveMinSize(int minSize) {
      if (minSize == COMPACT) {
        return 1;
      }
      return Math.max(minSize, 0);
    }

    // WHY effectiveMinSize: upstream turned every non-compact minimum into 0, so a minimum of 2 accepted empty arrays
    @Override
    default void checkSize(String key, int size, ErrorFactory error) {
      int minSize = effectiveMinSize(minSize());
      int maxSize = maxSize();
      if (size < minSize || maxSize < size) {
        if (maxSize == Integer.MAX_VALUE) {
          throw error.create(key + " must have at least " + minSize + " elements");
        }
        throw error.create(key + " must have between " + minSize + " and " + maxSize + " elements");
      }
    }
  }
}
