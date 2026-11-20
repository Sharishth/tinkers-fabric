package slimeknights.mantle.data.datamap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.listener.JsonContextReloadListener;
import slimeknights.mantle.data.listener.ReloadContext;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * Loader mapping block states to JSON data.
 * Each block reads the file named after its id, shaped like a block state file: {@code variants} maps a variant key
 * such as {@code facing=north} to the data, or to the id of another file in the folder holding it.
 * Any primitive variant value is read as such an id, so the data itself has to be a JSON object or array.
 * Values parse with the reload's parse context (D-21).
 * @param <T>  Data type
 */
public class BlockStateDataMapLoader<T> extends JsonContextReloadListener {
  private final String name;
  @Getter
  private final Loadable<T> dataLoader;
  private Map<BlockState, T> dataMap = Map.of();

  /**
   * Creates a loader.
   * @param name        Name for error messages
   * @param packType    Pack type the loader is registered for
   * @param folder      Folder to load data from
   * @param dataLoader  Loadable for parsing values
   */
  public BlockStateDataMapLoader(String name, PackType packType, String folder, Loadable<T> dataLoader) {
    super(packType, folder);
    this.name = name;
    this.dataLoader = dataLoader;
  }

  /**
   * Gets the loadable used for one reload.
   * Subclasses override this when parsing needs the other files, such as defaults read from a shared file.
   * @param jsons  Every loaded file
   */
  protected Loadable<T> prepareLoader(Map<Identifier, JsonElement> jsons) {
    return dataLoader;
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> jsons, ResourceManager manager, TypedMap context) {
    long time = System.nanoTime();
    Map<BlockState, T> dataMap = new HashMap<>();
    Parser<T> parser = new Parser<>(name, jsons, prepareLoader(jsons), context, new HashMap<>());

    // only files named after a block become map entries; other files exist to be redirected to
    for (Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
      Identifier location = entry.getKey().identifier();
      JsonElement element = jsons.get(location);
      if (element == null) {
        continue;
      }
      try {
        // parse the whole block before adding it, so a broken file never leaves a partial list of states
        dataMap.putAll(parser.parseBlock(entry.getValue(), location, element));
      } catch (RuntimeException e) {
        Mantle.logger.error("Failed to parse {} data for {}", name, location, e);
      }
    }
    this.dataMap = dataMap;
    Mantle.logger.info("Finished loading {} {} in {} ms", dataMap.size(), name, (System.nanoTime() - time) / 1000000f);
  }

  /**
   * State of one reload.
   * @param name         Name for error messages
   * @param jsons        Every loaded file
   * @param dataLoader   Loadable for parsing values
   * @param context      Parse context of the reload
   * @param locationMap  Parsed data per redirect target, so blocks sharing a file share one instance
   */
  private record Parser<T>(String name, Map<Identifier, JsonElement> jsons, Loadable<T> dataLoader, TypedMap context, Map<Identifier, T> locationMap) {
    /** Parses the file of one block into data per state; an empty object disables the block */
    Map<BlockState, T> parseBlock(Block block, Identifier location, JsonElement element) {
      Map<BlockState, T> states = new HashMap<>();
      JsonObject json = GsonHelper.convertToJsonObject(element, location.toString());
      if (json.keySet().isEmpty()) {
        return states;
      }
      StateDefinition<Block, BlockState> container = block.getStateDefinition();
      TypedMap blockContext = ReloadContext.withDebug(context, name + ' ' + location);
      for (Entry<String, JsonElement> variant : GsonHelper.getAsJsonObject(json, "variants").entrySet()) {
        String key = variant.getKey();
        T data = parseVariant(variant.getValue(), key, location, blockContext);
        Predicate<BlockState> matches = StateVariantStringBuilder.predicate(container, key);
        for (BlockState state : container.getPossibleStates()) {
          if (matches.test(state)) {
            states.put(state, data);
          }
        }
      }
      return states;
    }

    /** Parses one variant value: a string redirects to another file, anything else is the data itself */
    private T parseVariant(JsonElement variantElement, String key, Identifier location, TypedMap blockContext) {
      if (!variantElement.isJsonPrimitive()) {
        return dataLoader.convert(variantElement, key, blockContext);
      }
      Identifier parent = JsonHelper.convertToIdentifier(variantElement, key);
      T data = locationMap.get(parent);
      if (data == null) {
        JsonElement parentElement = jsons.get(parent);
        if (parentElement == null) {
          throw new JsonSyntaxException("Missing parent at " + parent + " for " + name + ", used in " + location);
        }
        data = dataLoader.convert(parentElement, parent.toString(), ReloadContext.withDebug(context, name + ' ' + parent));
        locationMap.put(parent, data);
      }
      return data;
    }
  }

  /** Gets the data of a block state, or null when it has none */
  public @Nullable T get(BlockState state) {
    return dataMap.get(state);
  }

  /** Gets the data of a block state, or the default when it has none */
  public T get(BlockState state, T defaultValue) {
    return dataMap.getOrDefault(state, defaultValue);
  }
}
