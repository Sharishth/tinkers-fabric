package slimeknights.mantle.util;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Small logic helpers.
 * Not ported: orElseNull for Forge LazyOptional, as Fabric has no capabilities.
 */
public final class LogicHelper {
  private LogicHelper() {}

  /** Returns the fallback when the value equals the undesired one, otherwise the value */
  public static int defaultIf(int check, int undesired, int fallback) {
    return check == undesired ? fallback : check;
  }

  /** Gets a list element, or the default when the index is out of range */
  public static <E> @Nullable E getOrDefault(List<E> list, int index, @Nullable E defaultValue) {
    if (index < 0 || index >= list.size()) {
      return defaultValue;
    }
    return list.get(index);
  }

  /** Checks if the array contains an element equal to the value */
  public static <T> boolean isInList(T[] values, T value) {
    for (T element : values) {
      if (value.equals(element)) {
        return true;
      }
    }
    return false;
  }
}
