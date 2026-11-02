package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.array.BooleanArrayLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Locale;
import java.util.function.Function;

/** Loadable for a boolean; a JSON boolean, or the string "true"/"false" as a map key */
public enum BooleanLoadable implements StringLoadable<Boolean> {
  INSTANCE;

  /** Same as {@link #INSTANCE}; hides {@link StringLoadable#DEFAULT}, which is a string loadable */
  @SuppressWarnings("unused")
  public static final BooleanLoadable DEFAULT = INSTANCE;

  @Override
  public Boolean convert(JsonElement element, String key, TypedMap context) {
    return GsonHelper.convertToBoolean(element, key);
  }

  @Override
  public JsonElement serialize(Boolean object) {
    return new JsonPrimitive(object);
  }

  @Override
  public Boolean decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return buffer.readBoolean();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Boolean object) {
    buffer.writeBoolean(object);
  }

  /** Booleans read better when written even at their default, so this default field always writes */
  @Override
  public <P> LoadableField<Boolean, P> defaultField(String key, Boolean defaultValue, Function<P, Boolean> getter) {
    return defaultField(key, defaultValue, true, getter);
  }

  /** Makes a boolean array loadable */
  public ArrayLoadable<boolean[]> array(int minSize, int maxSize) {
    return new BooleanArrayLoadable(this, minSize, maxSize);
  }

  /** Makes a boolean array loadable with no size limit */
  public ArrayLoadable<boolean[]> array(int minSize) {
    return array(minSize, Integer.MAX_VALUE);
  }

  /** WHY strict: Boolean.parseBoolean treats anything but "true" as false, hiding typos in data */
  @Override
  public Boolean parseString(String value, String key, TypedMap context) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new JsonSyntaxException("Invalid boolean '" + value + '\'');
    };
  }

  @Override
  public String getString(Boolean object) {
    return object.toString();
  }
}
