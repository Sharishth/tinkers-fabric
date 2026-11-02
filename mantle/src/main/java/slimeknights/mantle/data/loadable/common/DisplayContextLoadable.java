package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.EnumMapLoadable;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map;

/**
 * Loadable for an item display context, stored as {@code minecraft:<name>} as the Forge registry ids were.
 * WHY not EnumLoadable: 26.x display contexts are a plain enum (Forge's registry is gone), but their names
 * ({@code thirdperson_righthand}) differ from the enum constants, and old data uses the namespaced form.
 */
public enum DisplayContextLoadable implements IdentifierLoadable<ItemDisplayContext> {
  INSTANCE;

  @Override
  public ItemDisplayContext fromKey(Identifier name, String key, TypedMap context) {
    if (Identifier.DEFAULT_NAMESPACE.equals(name.getNamespace())) {
      for (ItemDisplayContext value : ItemDisplayContext.values()) {
        if (value.getSerializedName().equals(name.getPath())) {
          return value;
        }
      }
    }
    throw new JsonSyntaxException("Unable to parse " + key + " as there is no item display context " + name);
  }

  @Override
  public Identifier getKey(ItemDisplayContext object) {
    return Identifier.withDefaultNamespace(object.getSerializedName());
  }

  @Override
  public ItemDisplayContext decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int id = buffer.readVarInt();
    for (ItemDisplayContext value : ItemDisplayContext.values()) {
      if (value.getId() == id) {
        return value;
      }
    }
    throw new DecoderException("Unknown item display context id " + id);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, ItemDisplayContext value) {
    buffer.writeVarInt(value.getId());
  }

  @Override
  public <V> Loadable<Map<ItemDisplayContext, V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new EnumMapLoadable<>(ItemDisplayContext.class, this, valueLoadable, minSize);
  }
}
