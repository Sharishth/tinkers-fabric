package slimeknights.mantle.client;

import net.fabricmc.api.ClientModInitializer;
import slimeknights.mantle.Mantle;

/**
 * Client entrypoint for Mantle.
 * Keeps upstream's class name so the client wiring ported in M7 lands in the same place.
 */
public class ClientEvents implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // WHY no sign renderers or atlas hook: wood signs use vanilla's sign block entity types and renderers,
    // and 26.3 sign boards are plain block models, so upstream's Sheets.addWoodType has no job left
    // TODO(T-70): model loaders (connected, item_layer, colored_block, fallback, nbt_key, retextured)
    // TODO(T-72): reload listeners, offhand attack indicator, gauge tooltip, extra hearts
    // TODO(T-73): book loader and the test book
    Mantle.logger.info("Mantle client initialized");
  }
}
