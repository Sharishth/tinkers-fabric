package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;

/**
 * Loadable for holders of a datapack registry, such as enchantments or damage types.
 * New in the port (D-21): 26.x made these registries data-driven, so their values only exist per world.
 * JSON needs {@link slimeknights.mantle.data.loadable.field.ContextKey#REGISTRIES}; the network uses the buffer's registries.
 * @param <T>  Entry type
 */
public final class DynamicRegistryLoadable<T> implements IdentifierLoadable<Holder<T>> {
  private final ResourceKey<? extends Registry<T>> registryKey;
  private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> network;

  public DynamicRegistryLoadable(ResourceKey<? extends Registry<T>> registryKey) {
    this.registryKey = registryKey;
    this.network = ByteBufCodecs.holderRegistry(registryKey);
  }

  @Override
  public Holder<T> fromKey(Identifier name, String key, TypedMap context) {
    Optional<? extends HolderLookup.RegistryLookup<T>> registry = LoadableRegistries.lookup(context).lookup(registryKey);
    if (registry.isEmpty()) {
      throw new JsonSyntaxException("Unable to parse " + key + ": registry " + registryKey.identifier() + " is not available here, it needs world registries in the parse context");
    }
    return registry.get().get(ResourceKey.create(registryKey, name))
      .orElseThrow(() -> new JsonSyntaxException("Unable to parse " + key + " as registry " + registryKey.identifier() + " does not contain ID " + name));
  }

  @Override
  public Identifier getKey(Holder<T> holder) {
    return holder.unwrapKey()
      .orElseThrow(() -> new RuntimeException("Holder " + holder + " in " + registryKey.identifier() + " has no id"))
      .identifier();
  }

  @Override
  public Holder<T> decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return network.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Holder<T> value) {
    network.encode(buffer, value);
  }

  @Override
  public String toString() {
    return "DynamicRegistryLoadable[" + registryKey.identifier() + ']';
  }
}
