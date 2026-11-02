package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.mapping.SetLoadable;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Loadable for entries of a built-in registry: their id in JSON, their numeric id on the network.
 * Datapack registries such as enchantments need {@link DynamicRegistryLoadable} instead.
 * @param <T>  Entry type
 */
public interface BaseRegistryLoadable<T> extends IdentifierLoadable<T> {
  /** Gets the registry, or null when it cannot be found */
  @Nullable Registry<T> registry();

  /** Gets the registry id, for error messages */
  Identifier registryId();

  @Override
  default T fromKey(Identifier name, String key, TypedMap context) {
    Registry<T> registry = registry();
    if (registry != null) {
      T value = registry.getOptional(name).orElse(null);
      if (value != null) {
        return value;
      }
    }
    throw new JsonSyntaxException("Unable to parse " + key + " as registry " + registryId() + " does not contain ID " + name);
  }

  @Override
  default Identifier getKey(T object) {
    Registry<T> registry = registry();
    if (registry != null) {
      Identifier location = registry.getKey(object);
      if (location != null) {
        return location;
      }
    }
    throw new RuntimeException("Registry " + registryId() + " does not contain object " + object);
  }

  @Override
  default T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int id = buffer.readVarInt();
    Registry<T> registry = registry();
    if (registry != null) {
      T value = registry.byId(id);
      if (value != null) {
        return value;
      }
    }
    throw new DecoderException("Registry " + registryId() + " does not contain ID " + id);
  }

  @Override
  default void encode(RegistryFriendlyByteBuf buffer, T object) {
    Registry<T> registry = registry();
    if (registry == null) {
      throw new EncoderException("Registry " + registryId() + " cannot be located");
    }
    int id = registry.getId(object);
    if (id < 0) {
      throw new EncoderException("Registry " + registryId() + " does not contain object " + object);
    }
    buffer.writeVarInt(id);
  }

  /** WHY ordered: registry sets come from data, and keeping their order keeps output stable */
  @Override
  default SetLoadable<T> set(int minSize) {
    return new SetLoadable.Ordered<>(this, minSize);
  }
}
