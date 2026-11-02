package slimeknights.mantle;

import net.fabricmc.api.ModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central mod object for Mantle.
 * Fabric creates one instance through the "main" entrypoint in fabric.mod.json.
 */
public class Mantle implements ModInitializer {
  public static final String modId = "mantle";
  public static final Logger logger = LogManager.getLogger("Mantle");
  /**
   * Namespace for common tags.
   * WHY: upstream used "forge" on 1.20 and planned "c" for 1.21+; Fabric and 26.x vanilla use "c".
   */
  public static final String COMMON = "c";

  /** Instance of this mod, set once Fabric runs the entrypoint */
  public static Mantle instance;

  @Override
  public void onInitialize() {
    instance = this;
    // TODO(T-15): register client and server config (upstream Mantle constructor)
    // TODO(T-22): MantleTags.init (sign block entity types: vanilla's, see registerWood)
    // TODO(T-13): predicate loaders and mob type predicates (upstream register RECIPE_SERIALIZER block)
    // TODO(T-16): MantleNetwork.registerPackets (upstream commonSetup)
    // TODO(T-17): MantleCommand.init and resource_or_tag_key argument type
    // TODO(T-18): soulbound events (MantleEvents) and OffhandCooldownTracker
    // TODO(T-44): recipe conditions, ingredients, TagPreference.init
    // TODO(T-45): LootTableInjector.init and global loot modifiers
    // TODO(T-50): FluidContainerTransferManager init and transfer deserializers
    // TODO(T-73): LecternBookItem right click on lecterns
    logger.info("Mantle initialized");
  }

  /**
   * Gets an identifier in Mantle's namespace.
   * @param name  Path
   * @return  Identifier instance
   */
  public static Identifier getResource(String name) {
    return Identifier.fromNamespaceAndPath(modId, name);
  }

  /**
   * Gets an identifier in the common tag namespace.
   * @param name  Path
   * @return  Identifier instance
   */
  public static Identifier commonResource(String name) {
    return Identifier.fromNamespaceAndPath(COMMON, name);
  }

  /**
   * Makes a translation key for the given name.
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static String makeDescriptionId(String base, String name) {
    return Util.makeDescriptionId(base, getResource(name));
  }

  /**
   * Makes a translation text component for the given name.
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translatable component
   */
  public static MutableComponent makeComponent(String base, String name) {
    return Component.translatable(makeDescriptionId(base, name));
  }

  /**
   * Makes a formatted translation text component for the given name.
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @param args  Additional arguments to format strings
   * @return  Translatable component
   */
  public static MutableComponent makeComponent(String base, String name, Object... args) {
    return Component.translatable(makeDescriptionId(base, name), args);
  }
}
