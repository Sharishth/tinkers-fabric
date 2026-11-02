package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable backed by a vanilla codec, with registry access in JSON (D-21).
 * @param codec   Codec for JSON
 * @param stream  Codec for the network
 */
public record CodecLoadable<T>(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> stream) implements Loadable<T> {
  /** Creates a loadable that sends values as NBT; prefer the type's own stream codec when it has one */
  public CodecLoadable(Codec<T> codec) {
    this(codec, ByteBufCodecs.fromCodecWithRegistries(codec));
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return codec.parse(LoadableRegistries.jsonOps(context), element).getOrThrow(error -> new JsonSyntaxException("Failed to parse " + key + ": " + error));
  }

  @Override
  public JsonElement serialize(T object) {
    return codec.encodeStart(LoadableRegistries.jsonOps(TypedMap.EMPTY), object).getOrThrow(error -> new RuntimeException("Failed to serialize " + object + ": " + error));
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return stream.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T object) {
    stream.encode(buffer, object);
  }
}
