package slimeknights.mantle.data.loadable.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable backed by a Gson serializer; sent on the network as NBT converted from its JSON.
 * @param gson       Gson with the serializer registered
 * @param classType  Class to read and write; must serialize to a JSON object
 */
public record GsonLoadable<T>(Gson gson, Class<T> classType) implements Loadable<T> {
  @Override
  public T convert(JsonElement json, String key, TypedMap context) {
    return gson.fromJson(json, classType);
  }

  @Override
  public JsonElement serialize(T object) {
    return gson.toJsonTree(object, classType);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    CompoundTag tag = buffer.readNbt();
    if (tag == null) {
      throw new DecoderException("Failed to decode: " + classType.getSimpleName());
    }
    return gson.fromJson(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag), classType);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T object) {
    Tag tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, gson.toJsonTree(object, classType));
    if (!(tag instanceof CompoundTag compound)) {
      throw new EncoderException("Serialized wrong NBT tag type " + tag + ", " + classType.getSimpleName() + " must serialize to a JSON object");
    }
    buffer.writeNbt(compound);
  }
}
