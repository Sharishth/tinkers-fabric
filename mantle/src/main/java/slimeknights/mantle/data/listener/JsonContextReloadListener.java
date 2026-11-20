package slimeknights.mantle.data.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.typed.TypedMap;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * JSON folder listener that applies with the parse context.
 * Replaces vanilla's {@link net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener} for Mantle loaders:
 * 26.x made that class codec based and its apply step cannot see the reload's shared state, so it has no way to give
 * loadables the world registries (D-21).
 * Files are read in the prepare phase off the game thread; {@link #apply} runs on the game thread.
 */
@RequiredArgsConstructor
public abstract class JsonContextReloadListener implements PreparableReloadListener {
  /** Pack type this loader is registered for; decides whether the context has registries */
  @Getter
  private final PackType packType;
  /** Folder the JSON files are read from */
  @Getter
  private final String folder;

  @Override
  public final CompletableFuture<Void> reload(SharedState state, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
    ResourceManager manager = state.resourceManager();
    return CompletableFuture.supplyAsync(() -> readFolder(manager, folder), prepareExecutor)
      .thenCompose(barrier::wait)
      .thenAcceptAsync(jsons -> apply(jsons, manager, ReloadContext.create(state, packType)), applyExecutor);
  }

  /**
   * Applies the loaded files; runs on the game thread.
   * @param jsons    Top pack's JSON per id; the listener owns the map and may change it
   * @param manager  Resource manager
   * @param context  Parse context of the reload, see {@link ReloadContext}
   */
  protected abstract void apply(Map<Identifier, JsonElement> jsons, ResourceManager manager, TypedMap context);

  /**
   * Reads the top pack's copy of every JSON file in a folder.
   * A file that cannot be read is logged and left out, so one bad file does not fail the reload.
   * @param manager  Resource manager to read from
   * @param folder   Folder to search, without namespace
   * @return  Mutable map of id, the path inside the folder without extension, to JSON
   */
  public static Map<Identifier, JsonElement> readFolder(ResourceManager manager, String folder) {
    FileToIdConverter converter = FileToIdConverter.json(folder);
    Map<Identifier, JsonElement> jsons = new HashMap<>();
    for (Entry<Identifier, Resource> entry : converter.listMatchingResources(manager).entrySet()) {
      Identifier filePath = entry.getKey();
      try (Reader reader = entry.getValue().openAsReader()) {
        jsons.put(converter.fileToId(filePath), StrictJsonParser.parse(reader));
      } catch (JsonParseException | IOException ex) {
        Mantle.logger.error("Couldn't read data file {} from pack {}", filePath, entry.getValue().sourcePackId(), ex);
      }
    }
    return jsons;
  }
}
