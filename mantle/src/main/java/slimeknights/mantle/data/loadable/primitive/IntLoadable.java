package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.array.ByteArrayLoadable;
import slimeknights.mantle.data.loadable.array.IntArrayLoadable;
import slimeknights.mantle.data.loadable.array.ShortArrayLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable for an int within a range, both ends included.
 * The network form is the smallest encoding that fits the range.
 */
@SuppressWarnings("unused")  // API
@RequiredArgsConstructor
public class IntLoadable implements Loadable<Integer> {
  /** Any int */
  public static final IntLoadable ANY_FULL = range(Integer.MIN_VALUE, Integer.MAX_VALUE);
  /** Any value that fits a short */
  public static final IntLoadable ANY_SHORT = range(Short.MIN_VALUE, Short.MAX_VALUE);
  /** Any value that fits a byte */
  public static final IntLoadable ANY_BYTE = range(Byte.MIN_VALUE, Byte.MAX_VALUE);
  /** -1 up to the short limit, for values where -1 means unset */
  public static final IntLoadable FROM_MINUS_ONE = range(-1, Short.MAX_VALUE);
  /** Zero or more */
  public static final IntLoadable FROM_ZERO = min(0);
  /** One or more */
  public static final IntLoadable FROM_ONE = min(1);

  private final int min;
  private final int max;
  private final IntNetwork network;

  /** Creates a loadable for the given range */
  public static IntLoadable range(int min, int max) {
    return new IntLoadable(min, max, IntNetwork.recommended(min, max));
  }

  /** Creates a loadable with only a lower bound */
  public static IntLoadable min(int min) {
    return range(min, Integer.MAX_VALUE);
  }

  /** Returns the value when in range, otherwise throws naming the key */
  protected int validate(int value, String key) {
    if (min <= value && value <= max) {
      return value;
    }
    throw RangeError.create(key, min, max, min != Integer.MIN_VALUE, max != Integer.MAX_VALUE);
  }

  @Override
  public Integer convert(JsonElement element, String key, TypedMap context) {
    return validate(GsonHelper.convertToInt(element, key), key);
  }

  @Override
  public JsonElement serialize(Integer value) {
    return new JsonPrimitive(validate(value, "Value"));
  }

  @Override
  public Integer decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return network.read(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Integer object) {
    network.write(object, buffer);
  }


  /* Arrays */

  /** Makes an int array loadable */
  public ArrayLoadable<int[]> array(int minSize, int maxSize) {
    return new IntArrayLoadable(this, minSize, maxSize);
  }

  /** Makes an int array loadable with no size limit */
  public ArrayLoadable<int[]> array(int minSize) {
    return array(minSize, Integer.MAX_VALUE);
  }

  /** Makes a short array loadable; values outside the short range are cut down to 16 bits */
  public ArrayLoadable<short[]> shortArray(int minSize, int maxSize) {
    return new ShortArrayLoadable<>(this, minSize, maxSize, Integer::valueOf);
  }

  /** Makes a short array loadable with no size limit */
  public ArrayLoadable<short[]> shortArray(int minSize) {
    return shortArray(minSize, Integer.MAX_VALUE);
  }

  /** Makes a byte array loadable; values outside the byte range are cut down to 8 bits */
  public ArrayLoadable<byte[]> byteArray(int minSize, int maxSize) {
    return new ByteArrayLoadable<>(this, minSize, maxSize, Integer::valueOf);
  }

  /** Makes a byte array loadable with no size limit */
  public ArrayLoadable<byte[]> byteArray(int minSize) {
    return byteArray(minSize, Integer.MAX_VALUE);
  }


  /* Strings */

  /**
   * Makes a loadable that stores the int as a string, so it can be a map key.
   * @param radix  Number base; 10 matches plain JSON numbers
   */
  public StringLoadable<Integer> asString(int radix) {
    return new StringIntLoadable(min, max, radix, network);
  }

  /** Network encodings for an int */
  public enum IntNetwork {
    INT {
      @Override
      int read(RegistryFriendlyByteBuf buffer) {
        return buffer.readInt();
      }

      @Override
      void write(int value, RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(value);
      }
    },
    VAR_INT {
      @Override
      int read(RegistryFriendlyByteBuf buffer) {
        return buffer.readVarInt();
      }

      @Override
      void write(int value, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(value);
      }
    },
    SHORT {
      @Override
      int read(RegistryFriendlyByteBuf buffer) {
        return buffer.readShort();
      }

      @Override
      void write(int value, RegistryFriendlyByteBuf buffer) {
        buffer.writeShort(value);
      }
    },
    BYTE {
      @Override
      int read(RegistryFriendlyByteBuf buffer) {
        return buffer.readByte();
      }

      @Override
      void write(int value, RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(value);
      }
    };

    /** Reads an int in this encoding */
    abstract int read(RegistryFriendlyByteBuf buffer);

    /** Writes an int in this encoding */
    abstract void write(int value, RegistryFriendlyByteBuf buffer);

    /**
     * Picks the smallest encoding for a range.
     * WHY var int only from zero: var ints take 5 bytes for any negative value.
     */
    public static IntNetwork recommended(int min, int max) {
      if (min >= 0) {
        return VAR_INT;
      }
      if (min >= Byte.MIN_VALUE && max <= Byte.MAX_VALUE) {
        return BYTE;
      }
      if (min >= Short.MIN_VALUE && max <= Short.MAX_VALUE) {
        return SHORT;
      }
      return INT;
    }
  }

  /** Int stored as a string in the given base */
  private static class StringIntLoadable extends IntLoadable implements StringLoadable<Integer> {
    private final int radix;

    StringIntLoadable(int min, int max, int radix, IntNetwork network) {
      super(min, max, network);
      if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
        throw new IllegalArgumentException("Invalid radix " + radix + ", must be between " + Character.MIN_RADIX + " and " + Character.MAX_RADIX);
      }
      this.radix = radix;
    }

    @Override
    public Integer parseString(String value, String key, TypedMap context) {
      try {
        return validate(Integer.parseInt(value, radix), key);
      } catch (NumberFormatException e) {
        throw new JsonSyntaxException("Failed to parse integer at " + key, e);
      }
    }

    @Override
    public Integer convert(JsonElement element, String key, TypedMap context) {
      return parseString(GsonHelper.convertToString(element, key), key, context);
    }

    @Override
    public String getString(Integer value) {
      return Integer.toString(validate(value, "Value"), radix);
    }

    @Override
    public JsonElement serialize(Integer value) {
      return new JsonPrimitive(getString(value));
    }
  }
}
