package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.array.LongArrayLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Loadable for a long within a range, both ends included */
@SuppressWarnings("unused")  // API
@RequiredArgsConstructor
public class LongLoadable implements Loadable<Long> {
  /** Any long */
  public static final LongLoadable ANY = range(Long.MIN_VALUE, Long.MAX_VALUE);
  /** Zero or more */
  public static final LongLoadable FROM_ZERO = min(0);
  /** One or more */
  public static final LongLoadable FROM_ONE = min(1);

  private final long min;
  private final long max;
  /** If true, sent as a var long, otherwise as 8 bytes */
  private final boolean isVarLong;

  /** Creates a loadable for the given range, sent as a var long */
  public static LongLoadable range(long min, long max) {
    return new LongLoadable(min, max, true);
  }

  /** Creates a loadable with only a lower bound */
  public static LongLoadable min(long min) {
    return range(min, Long.MAX_VALUE);
  }

  /** Returns the value when in range, otherwise throws naming the key */
  protected long validate(long value, String key) {
    if (min <= value && value <= max) {
      return value;
    }
    throw RangeError.create(key, min, max, min != Long.MIN_VALUE, max != Long.MAX_VALUE);
  }

  @Override
  public Long convert(JsonElement element, String key, TypedMap context) {
    return validate(GsonHelper.convertToLong(element, key), key);
  }

  @Override
  public JsonElement serialize(Long value) {
    return new JsonPrimitive(validate(value, "Value"));
  }

  @Override
  public Long decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return isVarLong ? buffer.readVarLong() : buffer.readLong();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Long value) {
    if (isVarLong) {
      buffer.writeVarLong(value);
    } else {
      buffer.writeLong(value);
    }
  }

  /** Makes a long array loadable */
  public ArrayLoadable<long[]> array(int minSize, int maxSize) {
    return new LongArrayLoadable(this, minSize, maxSize);
  }

  /** Makes a long array loadable with no size limit */
  public ArrayLoadable<long[]> array(int minSize) {
    return array(minSize, Integer.MAX_VALUE);
  }

  /**
   * Makes a loadable that stores the long as a string, so it can be a map key.
   * @param radix  Number base; 10 matches plain JSON numbers
   */
  public StringLoadable<Long> asString(int radix) {
    return new StringLongLoadable(min, max, radix, isVarLong);
  }

  /** Long stored as a string in the given base */
  private static class StringLongLoadable extends LongLoadable implements StringLoadable<Long> {
    private final int radix;

    StringLongLoadable(long min, long max, int radix, boolean isVarLong) {
      super(min, max, isVarLong);
      if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
        throw new IllegalArgumentException("Invalid radix " + radix + ", must be between " + Character.MIN_RADIX + " and " + Character.MAX_RADIX);
      }
      this.radix = radix;
    }

    @Override
    public Long parseString(String value, String key, TypedMap context) {
      try {
        return validate(Long.parseLong(value, radix), key);
      } catch (NumberFormatException e) {
        throw new JsonSyntaxException("Failed to parse long at " + key, e);
      }
    }

    @Override
    public Long convert(JsonElement element, String key, TypedMap context) {
      return parseString(GsonHelper.convertToString(element, key), key, context);
    }

    @Override
    public String getString(Long value) {
      return Long.toString(validate(value, "Value"), radix);
    }

    @Override
    public JsonElement serialize(Long value) {
      return new JsonPrimitive(getString(value));
    }
  }
}
