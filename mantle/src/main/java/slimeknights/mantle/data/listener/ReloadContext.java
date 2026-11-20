package slimeknights.mantle.data.listener;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

/**
 * Builds the loadable parse context of a reload.
 * A server data reload puts its registries under {@link ContextKey#REGISTRIES} (D-21), so loadables resolve datapack
 * registries such as enchantments, and tags as the packs being loaded define them.
 * Client resources have no world registries; their loadables fall back to the built-in registries.
 */
public final class ReloadContext {
  private ReloadContext() {}

  /**
   * Gets the registries of a reload.
   * WHY the pack type: {@code SharedState.get} throws on a missing key, and Fabric only sets the registry lookup
   * for server data, so the listener has to say which kind of reload it was registered for.
   * @param state  State shared by the listeners of the reload
   * @param type   Pack type the listener is registered for
   * @return  Registries including the tags being loaded, or null for client resources
   */
  public static HolderLookup.@Nullable Provider registries(SharedState state, PackType type) {
    if (type == PackType.SERVER_DATA) {
      return state.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
    }
    return null;
  }

  /**
   * Creates the parse context of a reload.
   * @param state  State shared by the listeners of the reload
   * @param type   Pack type the listener is registered for
   * @return  Context holding the registries for server data, empty for client resources
   */
  public static TypedMap create(SharedState state, PackType type) {
    HolderLookup.Provider registries = registries(state, type);
    if (registries == null) {
      return TypedMap.EMPTY;
    }
    return TypedMapBuilder.builder().put(ContextKey.REGISTRIES, registries).build();
  }

  /**
   * Copies a parse context, adding text that names what is being parsed.
   * @param context  Context of the reload
   * @param debug    Text for error messages, such as the loader name and file id
   */
  public static TypedMap withDebug(TypedMap context, String debug) {
    return TypedMapBuilder.builder().putAll(context).put(ContextKey.DEBUG, debug).build();
  }
}
