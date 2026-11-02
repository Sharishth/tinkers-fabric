package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * One field of a {@link RecordLoadable}: reads its value and writes it from the parent object.
 * Unlike {@link LoadableField}, not meant for use outside record loadables.
 * @param <T>  Field value type
 * @param <P>  Parent object type
 */
public interface RecordField<T, P> {
  /**
   * Reads the field from the parent JSON object.
   * @param context  Parse context; empty in nested objects unless {@link DirectField} passes it on
   * @throws com.google.gson.JsonSyntaxException  If the field is invalid
   */
  T get(JsonObject json, TypedMap context);

  /**
   * Writes the field of the parent into the JSON object.
   * @throws RuntimeException  If the value cannot be written
   */
  void serialize(P parent, JsonObject json);

  /**
   * Reads the field from the network.
   * @param context  Parse context; empty in nested objects unless {@link DirectField} passes it on
   * @throws io.netty.handler.codec.DecoderException  If the value cannot be read
   */
  T decode(RegistryFriendlyByteBuf buffer, TypedMap context);

  /**
   * Writes the field of the parent to the network.
   * @throws io.netty.handler.codec.EncoderException  If the value cannot be written
   */
  void encode(RegistryFriendlyByteBuf buffer, P parent);
}
