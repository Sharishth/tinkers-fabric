package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Loadable for an NBT compound, stored as the equivalent JSON object.
 * Item data is a data component in 26.x; this remains for other NBT, such as block entity or entity data.
 */
public enum NBTLoadable implements RecordLoadable<CompoundTag> {
  /** Only a JSON object */
  DISALLOW_STRING,
  /** A JSON object, or an SNBT string such as {@code "{Count:2b}"} */
  ALLOW_STRING;

  @Override
  public CompoundTag deserialize(JsonObject json, TypedMap context) {
    return (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
  }

  // WHY convertToString: upstream ran the element back through Gson, which quotes strings and broke SNBT parsing
  @Override
  public CompoundTag convert(JsonElement element, String key, TypedMap context) {
    if (this == ALLOW_STRING && !element.isJsonObject()) {
      try {
        return TagParser.parseCompoundFully(GsonHelper.convertToString(element, key));
      } catch (CommandSyntaxException e) {
        throw new JsonSyntaxException("Invalid NBT Entry at " + key + ": " + e.getMessage(), e);
      }
    }
    return RecordLoadable.super.convert(element, key, context);
  }

  @Override
  public JsonObject serialize(CompoundTag object) {
    return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, object).getAsJsonObject();
  }

  @Override
  public void serialize(CompoundTag object, JsonObject json) {
    serialize(object).entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
  }

  @Override
  public CompoundTag decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    CompoundTag tag = buffer.readNbt();
    return tag != null ? tag : new CompoundTag();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, CompoundTag object) {
    buffer.writeNbt(object);
  }

  /** WHY its own field: NBT already encodes null on the network, so no presence flag is needed */
  @Override
  public <P> LoadableField<CompoundTag, P> nullableField(String key, Function<P, @Nullable CompoundTag> getter) {
    return new NullableNBTField<>(this, key, getter);
  }

  /** Nullable NBT field using NBT's own null on the network */
  private record NullableNBTField<P>(Loadable<CompoundTag> loadable, String key, Function<P, @Nullable CompoundTag> getter) implements LoadableField<CompoundTag, P> {
    @Override
    public @Nullable CompoundTag get(JsonObject json, String key, TypedMap context) {
      return loadable.getOrDefault(json, key, null, context);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
      CompoundTag nbt = getter.apply(parent);
      if (nbt != null) {
        json.add(key, loadable.serialize(nbt));
      }
    }

    @Override
    public @Nullable CompoundTag decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return buffer.readNbt();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, P parent) {
      buffer.writeNbt(getter.apply(parent));
    }
  }
}
