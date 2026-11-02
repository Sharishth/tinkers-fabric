package slimeknights.mantle.data.loadable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Network half of a loadable: a stream codec that can also read with parse context.
 * WHY RegistryFriendlyByteBuf (D-20): 26.x item, ingredient and fluid stream codecs need registry access, and play-phase
 * payloads always carry it, so taking it here makes a wrong buffer a compile error instead of a runtime cast failure.
 * @param <T>  Value type
 */
public interface Streamable<T> extends StreamCodec<RegistryFriendlyByteBuf, T> {
  /**
   * Reads a value from the network.
   * @param buffer   Buffer to read
   * @param context  Parse context, notably the recipe id for recipe serializers
   * @return  Read value
   * @throws io.netty.handler.codec.DecoderException  If the value cannot be read
   */
  T decode(RegistryFriendlyByteBuf buffer, TypedMap context);

  /** Reads a value with empty context */
  @Override
  default T decode(RegistryFriendlyByteBuf buffer) {
    return decode(buffer, TypedMap.EMPTY);
  }

  /**
   * Writes a value to the network.
   * @throws io.netty.handler.codec.EncoderException  If the value cannot be written
   */
  @Override
  void encode(RegistryFriendlyByteBuf buffer, T value);
}
