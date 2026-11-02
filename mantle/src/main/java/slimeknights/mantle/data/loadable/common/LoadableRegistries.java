package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Registry access for loadables whose JSON references registries (D-21).
 * Uses {@link ContextKey#REGISTRIES} when the caller provides it, otherwise the built-in registries,
 * which cover items, blocks and their tags but not datapack registries such as enchantments.
 */
public final class LoadableRegistries {
  private LoadableRegistries() {}

  /** Built-in registries, created on first use since they need the game bootstrapped */
  private static class BuiltIn {
    private static final HolderLookup.Provider LOOKUP = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
  }

  /** Gets the registries from the context, or the built-in registries */
  public static HolderLookup.Provider lookup(TypedMap context) {
    HolderLookup.Provider provider = context.get(ContextKey.REGISTRIES);
    return provider != null ? provider : BuiltIn.LOOKUP;
  }

  /** Gets JSON ops that can resolve registry references, for codec-backed loadables */
  public static RegistryOps<JsonElement> jsonOps(TypedMap context) {
    return lookup(context).createSerializationContext(JsonOps.INSTANCE);
  }
}
