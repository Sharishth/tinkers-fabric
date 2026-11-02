package slimeknights.mantle.data.loadable.common;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;

/**
 * Loadable for entries of a built-in registry that may be registered after the loadable is created,
 * such as a registry added by another mod.
 */
@RequiredArgsConstructor
public class LazyRegistryLoadable<T> implements BaseRegistryLoadable<T> {
  private final ResourceKey<? extends Registry<T>> registryKey;
  /** Registry once found; a miss is not cached, so a late registry is still found */
  private @Nullable Registry<T> registry;

  @SuppressWarnings("unchecked")  // the registry key names a registry of T
  @Override
  public @Nullable Registry<T> registry() {
    if (registry == null) {
      registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
      if (registry == null) {
        Mantle.logger.error("Registry {} cannot be located, treating as an empty registry. This may cause serialization issues down the line.", registryKey);
      }
    }
    return registry;
  }

  @Override
  public Identifier registryId() {
    return registryKey.identifier();
  }
}
