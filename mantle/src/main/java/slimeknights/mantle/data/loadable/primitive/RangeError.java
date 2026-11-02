package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonSyntaxException;

/** Builds the out-of-range error shared by the number loadables */
final class RangeError {
  private RangeError() {}

  /**
   * Creates the error for a value outside its range, naming only the bounds that apply.
   * @param key          JSON key of the value
   * @param min          Lower bound, as text
   * @param max          Upper bound, as text
   * @param hasMinLimit  False when the lower bound is the type's smallest value
   * @param hasMaxLimit  False when the upper bound is the type's largest value
   */
  static JsonSyntaxException create(String key, Object min, Object max, boolean hasMinLimit, boolean hasMaxLimit) {
    if (!hasMinLimit) {
      return new JsonSyntaxException(key + " must not be greater than " + max);
    }
    if (!hasMaxLimit) {
      return new JsonSyntaxException(key + " must not be less than " + min);
    }
    return new JsonSyntaxException(key + " must be between " + min + " and " + max);
  }
}
