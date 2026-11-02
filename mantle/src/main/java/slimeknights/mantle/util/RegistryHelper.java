package slimeknights.mantle.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helpers for built-in registries and their tags.
 * Upstream's per-type contains overloads (block, item, entity, fluid) are covered by the generic one,
 * as 26.x deprecates the builtInRegistryHolder they used.
 */
@SuppressWarnings("unused")  // API
public final class RegistryHelper {
  private RegistryHelper() {}

  /** Gets a built-in registry by key, or null when it does not exist */
  @SuppressWarnings("unchecked")  // the key names a registry of T
  public static <T> @Nullable Registry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
    return (Registry<T>) BuiltInRegistries.REGISTRY.getValue(key.identifier());
  }

  /** Gets the holders in a tag of the registry */
  public static <T> Stream<Holder<T>> getTagStream(Registry<T> registry, TagKey<T> key) {
    return StreamSupport.stream(registry.getTagOrEmpty(key).spliterator(), false);
  }

  /** Gets the holders in a tag of its built-in registry, or nothing if the registry does not exist */
  public static <T> Stream<Holder<T>> getTagStream(TagKey<T> key) {
    Registry<T> registry = getRegistry(key.registry());
    return registry == null ? Stream.empty() : getTagStream(registry, key);
  }

  /** Gets the bound values in a tag of the registry */
  public static <T> Stream<T> getTagValueStream(Registry<T> registry, TagKey<T> key) {
    return getTagStream(registry, key).filter(Holder::isBound).map(Holder::value);
  }

  /** Gets the bound values in a tag of its built-in registry */
  public static <T> Stream<T> getTagValueStream(TagKey<T> key) {
    return getTagStream(key).filter(Holder::isBound).map(Holder::value);
  }

  /** Checks if a registered value is in the tag; false for unregistered values */
  public static <T> boolean contains(Registry<T> registry, TagKey<T> tag, T value) {
    if (registry.getKey(value) == null) {
      return false;
    }
    return registry.wrapAsHolder(value).is(tag);
  }

  /** Checks if a value is in a tag of its built-in registry */
  public static <T> boolean contains(TagKey<T> tag, T value) {
    Registry<T> registry = getRegistry(tag.registry());
    return registry != null && contains(registry, tag, value);
  }

  /** Checks if a tag holds exactly the given entity types, in order */
  public static boolean isTagEquivalent(HolderSet.Named<EntityType<?>> tag, List<EntityType<?>> values) {
    int count = tag.size();
    if (count != values.size()) {
      return false;
    }
    for (int i = 0; i < count; i++) {
      if (!values.get(i).equals(tag.get(i).value())) {
        return false;
      }
    }
    return true;
  }
}
