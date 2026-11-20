package slimeknights.mantle.data.listener;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Unit;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.typed.TypedMap;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * JSON loader merging the same file from every pack.
 * Vanilla's {@link net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener} keeps only the top pack's
 * file; this one feeds each pack's copy, lowest priority first, into one builder per id.
 * Loads in the apply phase on the game thread, like upstream; {@code parse} and {@code finishLoad} receive the parse
 * context of the reload (D-21), which upstream did not have. Upstream's Gson argument is gone: files are read as
 * plain JSON elements, so the instance never mattered.
 * @param <B>  Builder class
 */
@RequiredArgsConstructor
public abstract class MergingJsonDataLoader<B> implements PreparableReloadListener {
  /** Pack type this loader is registered for; decides whether the context has registries */
  protected final PackType packType;
  @VisibleForTesting
  protected final String folder;
  @VisibleForTesting
  protected final Function<Identifier, B> builderConstructor;

  /**
   * Parses one pack's JSON into the builder.
   * @param builder  Builder shared by every pack's copy of the file
   * @param id       Id of the JSON being parsed
   * @param element  JSON data
   * @param context  Parse context of the reload, see {@link ReloadContext}
   * @throws JsonSyntaxException  If the JSON is invalid
   */
  protected abstract void parse(B builder, Identifier id, JsonElement element, TypedMap context) throws JsonSyntaxException;

  /**
   * Handles the builders once every file is parsed.
   * @param map      Builder per id
   * @param manager  Resource manager
   * @param context  Parse context of the reload, see {@link ReloadContext}
   */
  protected abstract void finishLoad(Map<Identifier, B> map, ResourceManager manager, TypedMap context);

  @Override
  public CompletableFuture<Void> reload(SharedState state, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
    return barrier.wait(Unit.INSTANCE)
      .thenRunAsync(() -> onResourceManagerReload(state.resourceManager(), ReloadContext.create(state, packType)), applyExecutor);
  }

  /**
   * Loads and merges every file, then finishes the load.
   * @param manager  Resource manager to read from
   * @param context  Parse context handed to {@code parse} and {@code finishLoad}
   */
  public void onResourceManagerReload(ResourceManager manager, TypedMap context) {
    FileToIdConverter converter = FileToIdConverter.json(folder);
    Map<Identifier, B> map = new HashMap<>();
    for (Entry<Identifier, List<Resource>> entry : converter.listMatchingResourceStacks(manager).entrySet()) {
      Identifier filePath = entry.getKey();
      Identifier id = converter.fileToId(filePath);
      for (Resource resource : entry.getValue()) {
        loadResource(map, id, filePath, resource, context);
      }
    }
    finishLoad(map, manager, context);
  }

  /** Parses one pack's copy of a file, logging instead of failing the reload */
  private void loadResource(Map<Identifier, B> map, Identifier id, Identifier filePath, Resource resource, TypedMap context) {
    try (Reader reader = resource.openAsReader()) {
      JsonElement json = StrictJsonParser.parse(reader);
      if (json.isJsonNull()) {
        Mantle.logger.error("Couldn't load data file {} from {} in data pack {} as its null or empty", id, filePath, resource.sourcePackId());
        return;
      }
      parse(map.computeIfAbsent(id, builderConstructor), id, json, context);
    } catch (RuntimeException | IOException ex) {
      Mantle.logger.error("Couldn't parse data file {} from {} in data pack {}", id, filePath, resource.sourcePackId(), ex);
    }
  }
}
