package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.array.DoubleArrayLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable for a double within a range, both ends included.
 * @param min  Smallest allowed value
 * @param max  Largest allowed value
 */
@SuppressWarnings("unused")  // API
public record DoubleLoadable(double min, double max) implements Loadable<Double> {
  /** Any double */
  public static final DoubleLoadable ANY = range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  /** Zero or more */
  public static final DoubleLoadable FROM_ZERO = min(0);
  /** Between 0 and 1 */
  public static final DoubleLoadable PERCENT = range(0, 1);

  /** Creates a loadable for the given range */
  public static DoubleLoadable range(double min, double max) {
    return new DoubleLoadable(min, max);
  }

  /** Creates a loadable with only a lower bound */
  public static DoubleLoadable min(double min) {
    return new DoubleLoadable(min, Double.POSITIVE_INFINITY);
  }

  /** Returns the value when in range, otherwise throws naming the key */
  private double validate(double value, String key) {
    if (min <= value && value <= max) {
      return value;
    }
    throw RangeError.create(key, min, max, min != Double.NEGATIVE_INFINITY, max != Double.POSITIVE_INFINITY);
  }

  @Override
  public Double convert(JsonElement element, String key, TypedMap context) {
    return validate(GsonHelper.convertToDouble(element, key), key);
  }

  @Override
  public JsonElement serialize(Double object) {
    return new JsonPrimitive(validate(object, "Value"));
  }

  @Override
  public Double decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return buffer.readDouble();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Double object) {
    buffer.writeDouble(object);
  }

  /** Makes a double array loadable */
  public ArrayLoadable<double[]> array(int minSize, int maxSize) {
    return new DoubleArrayLoadable(this, minSize, maxSize);
  }

  /** Makes a double array loadable with no size limit */
  public ArrayLoadable<double[]> array(int minSize) {
    return array(minSize, Integer.MAX_VALUE);
  }
}
