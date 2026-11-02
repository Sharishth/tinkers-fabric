package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Round-trip helpers for loadable unit tests */
public final class LoadableTesting {
  private LoadableTesting() {}

  /** Creates an empty network buffer with no registries, enough for loadables that do not touch registries */
  public static RegistryFriendlyByteBuf buffer() {
    return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
  }

  /** Creates a network buffer with the built-in registries, for items and other registry-backed values; needs a bootstrapped game */
  public static RegistryFriendlyByteBuf builtInBuffer() {
    return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
  }

  /** Writes the value with the codec, reads it back, and fails if bytes are left over */
  public static <T> T network(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
    return network(codec, value, buffer());
  }

  /** Same as {@link #network(StreamCodec, Object)} with the given buffer */
  public static <T> T network(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value, RegistryFriendlyByteBuf buffer) {
    codec.encode(buffer, value);
    T read = codec.decode(buffer);
    if (buffer.isReadable()) {
      throw new AssertionError(buffer.readableBytes() + " bytes left unread after decoding " + value);
    }
    return read;
  }

  /** Parses JSON text */
  public static JsonElement json(String text) {
    return JsonParser.parseString(text);
  }

  /** Writes the value to JSON and reads it back */
  public static <T> T json(Loadable<T> loadable, T value) {
    return loadable.convert(loadable.serialize(value), "test");
  }
}
