package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Round-trip helpers for loadable unit tests */
public final class LoadableTesting {
  private LoadableTesting() {}

  /** Creates an empty network buffer with no registries, enough for loadables that do not touch registries */
  public static RegistryFriendlyByteBuf buffer() {
    return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
  }

  /** Writes the value with the codec, reads it back, and fails if bytes are left over */
  public static <T> T network(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
    RegistryFriendlyByteBuf buffer = buffer();
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
