package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.array.FloatArrayLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable for a float within a range, both ends included.
 * @param min  Smallest allowed value
 * @param max  Largest allowed value
 */
public record FloatLoadable(float min, float max) implements Loadable<Float> {
  /** Any float */
  public static final FloatLoadable ANY = range(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
  /** Zero or more */
  public static final FloatLoadable FROM_ZERO = min(0);
  /** Between 0 and 1 */
  public static final FloatLoadable PERCENT = range(0, 1);

  /** Creates a loadable for the given range */
  public static FloatLoadable range(float min, float max) {
    return new FloatLoadable(min, max);
  }

  /** Creates a loadable with only a lower bound */
  public static FloatLoadable min(float min) {
    return new FloatLoadable(min, Float.POSITIVE_INFINITY);
  }

  /** Returns the value when in range, otherwise throws naming the key */
  private float validate(float value, String key) {
    if (min <= value && value <= max) {
      return value;
    }
    throw RangeError.create(key, min, max, min != Float.NEGATIVE_INFINITY, max != Float.POSITIVE_INFINITY);
  }

  @Override
  public Float convert(JsonElement element, String key, TypedMap context) {
    return validate(GsonHelper.convertToFloat(element, key), key);
  }

  @Override
  public JsonElement serialize(Float object) {
    return new JsonPrimitive(validate(object, "Value"));
  }

  @Override
  public Float decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return buffer.readFloat();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Float object) {
    buffer.writeFloat(object);
  }

  /** Makes a float array loadable */
  public ArrayLoadable<float[]> array(int minSize, int maxSize) {
    return new FloatArrayLoadable(this, minSize, maxSize);
  }

  /** Makes a float array loadable with no size limit */
  public ArrayLoadable<float[]> array(int minSize) {
    return array(minSize, Integer.MAX_VALUE);
  }
}
