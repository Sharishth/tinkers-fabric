package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Record field stored under one JSON key; also usable on its own to read a single field.
 * @param <T>  Field value type
 * @param <P>  Parent object type
 */
public interface LoadableField<T, P> extends RecordField<T, P> {
  /** Key the field is read from; legacy fields may fall back to others */
  String key();

  /**
   * Reads the field from a different key.
   * @param key  Key to use instead of {@link #key()}, used by legacy fallbacks
   * @throws com.google.gson.JsonSyntaxException  If the field is invalid
   */
  T get(JsonObject json, String key, TypedMap context);

  @Override
  default T get(JsonObject json, TypedMap context) {
    return get(json, key(), context);
  }

  /** Reads the field with empty context */
  default T get(JsonObject json) {
    return get(json, TypedMap.EMPTY);
  }

  /** Reads the field from the network with empty context */
  default T decode(RegistryFriendlyByteBuf buffer) {
    return decode(buffer, TypedMap.EMPTY);
  }
}
