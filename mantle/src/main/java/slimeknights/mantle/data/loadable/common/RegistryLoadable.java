package slimeknights.mantle.data.loadable.common;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * Loadable for entries of a built-in registry that exists when the loadable is created.
 * @param registry    Registry
 * @param registryId  Registry id, for error messages
 */
@SuppressWarnings("unused")  // API
public record RegistryLoadable<T>(Registry<T> registry, Identifier registryId) implements BaseRegistryLoadable<T> {
  /** Creates a loadable for the built-in registry with the given key */
  @SuppressWarnings("unchecked")  // the registry key names a registry of T
  public RegistryLoadable(ResourceKey<? extends Registry<T>> registryKey) {
    this(Objects.requireNonNull((Registry<T>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier()), "Unknown registry " + registryKey.identifier()),
         registryKey.identifier());
  }

  /** Creates a loadable for a built-in registry */
  public RegistryLoadable(Registry<T> registry) {
    this(registry, registry.key().identifier());
  }
}
