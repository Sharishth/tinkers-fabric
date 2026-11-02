package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Objects;
import java.util.function.Function;

/** Loadable for a packed ARGB color, stored as a hex string without a prefix */
public enum ColorLoadable implements StringLoadable<Integer> {
  /** Accepts RRGGBB (opaque) or AARRGGBB; writes AARRGGBB */
  ALPHA {
    @Override
    public Integer parseString(String color, String key, TypedMap context) {
      if (!color.isEmpty() && color.charAt(0) != '-') {
        try {
          // 8 digits overflow int, so parse as long and keep the low 32 bits
          if (color.length() == 8) {
            return (int) Long.parseLong(color, 16);
          }
          if (color.length() == 6) {
            return OPAQUE | Integer.parseInt(color, 16);
          }
        } catch (NumberFormatException ex) {
          // falls through to the invalid color error
        }
      }
      throw new JsonSyntaxException("Invalid color '" + color + "' at " + key);
    }

    @Override
    public String getString(Integer color) {
      return String.format("%08X", color);
    }
  },
  /** Accepts RRGGBB only and makes it opaque; writes RRGGBB */
  NO_ALPHA {
    @Override
    public Integer parseString(String color, String key, TypedMap context) {
      if (color.length() == 6 && color.charAt(0) != '-') {
        try {
          return OPAQUE | Integer.parseInt(color, 16);
        } catch (NumberFormatException ex) {
          // falls through to the invalid color error
        }
      }
      throw new JsonSyntaxException("Invalid color '" + color + "' at " + key);
    }

    @Override
    public String getString(Integer color) {
      return String.format("%06X", color & RGB_MASK);
    }
  };

  /** Full alpha */
  private static final int OPAQUE = 0xFF000000;
  /** Color bits without alpha */
  private static final int RGB_MASK = 0xFFFFFF;
  /** Opaque white, the usual "no tint" value */
  private static final int WHITE = -1;

  @Override
  public Integer decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return buffer.readInt();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Integer color) {
    buffer.writeInt(color);
  }

  /** Reads the color field, or opaque white when missing */
  public int getOrWhite(JsonObject parent, String key) {
    return Objects.requireNonNull(getOrDefault(parent, key, WHITE));
  }

  /** Creates a field that is opaque white when missing */
  public <P> LoadableField<Integer, P> defaultField(String key, boolean serializeDefault, Function<P, Integer> getter) {
    return StringLoadable.super.defaultField(key, WHITE, serializeDefault, getter);
  }
}
