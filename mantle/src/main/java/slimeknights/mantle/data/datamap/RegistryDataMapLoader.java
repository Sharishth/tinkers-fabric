package slimeknights.mantle.data.datamap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.listener.JsonContextReloadListener;
import slimeknights.mantle.data.listener.ReloadContext;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Loader mapping registry entries to JSON data.
 * Each entry reads the file named after its id; a {@code parent} key reuses data from another file, and an empty
 * object disables the entry. Values parse with the reload's parse context, so they may hold tags and datapack
 * registry entries (D-21).
 * @param <R>  Registry entry type, the map key
 * @param <D>  Data type, the map value
 */
public class RegistryDataMapLoader<R, D> extends JsonContextReloadListener {
  /** Default merger: copies every key the child lacks from the parent */
  public static final BiConsumer<JsonObject, JsonObject> COPY_PARENT_DATA = (json, parentJson) -> {
    for (Entry<String, JsonElement> entry : parentJson.entrySet()) {
      String key = entry.getKey();
      if (!json.has(key)) {
        json.add(key, entry.getValue());
      }
    }
  };
  /** JSON key naming the file to take data from */
  private static final String PARENT = "parent";

  private final String name;
  @Getter
  private final Registry<R> registry;
  @Getter
  private final RecordLoadable<D> dataLoader;
  private final BiConsumer<JsonObject, JsonObject> merger;
  private Map<R, D> dataMap = Map.of();

  /**
   * Creates a loader that copies missing keys from parents.
   * @param name        Name for error messages
   * @param packType    Pack type the loader is registered for
   * @param folder      Folder to load data from
   * @param registry    Registry whose entries are the keys of the map
   * @param dataLoader  Loadable for parsing values
   */
  public RegistryDataMapLoader(String name, PackType packType, String folder, Registry<R> registry, RecordLoadable<D> dataLoader) {
    this(name, packType, folder, registry, dataLoader, COPY_PARENT_DATA);
  }

  /**
   * Creates a loader with custom parent merging.
   * WHY 6 parameters: upstream's constructor plus the pack type; every one is needed to register the loader.
   * @param name        Name for error messages
   * @param packType    Pack type the loader is registered for
   * @param folder      Folder to load data from
   * @param registry    Registry whose entries are the keys of the map
   * @param dataLoader  Loadable for parsing values
   * @param merger      Copies data from the parent JSON (second) into the child JSON (first)
   */
  public RegistryDataMapLoader(String name, PackType packType, String folder, Registry<R> registry, RecordLoadable<D> dataLoader, BiConsumer<JsonObject, JsonObject> merger) {
    super(packType, folder);
    this.name = name;
    this.registry = registry;
    this.dataLoader = dataLoader;
    this.merger = merger;
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> jsons, ResourceManager manager, TypedMap context) {
    long time = System.nanoTime();
    Map<R, D> dataMap = new HashMap<>();
    // parsed data per file, so entries redirecting to the same file share one instance
    Map<Identifier, D> locationMap = new HashMap<>();

    // only files named after a registry entry become map entries; other files exist to be used as parents
    for (Entry<ResourceKey<R>, R> entry : registry.entrySet()) {
      Identifier location = entry.getKey().identifier();
      JsonElement element = jsons.get(location);
      if (element == null) {
        continue;
      }
      try {
        JsonObject json = GsonHelper.convertToJsonObject(element, location.toString());
        // an empty object is the way to disable an entry
        if (!json.keySet().isEmpty()) {
          TypedMap entryContext = ReloadContext.withDebug(context, name + ' ' + location);
          dataMap.put(entry.getValue(), parseData(name, jsons, location, json, locationMap, dataLoader, entryContext, merger));
        }
      } catch (RuntimeException e) {
        Mantle.logger.error("Failed to parse {} data for {}", name, location, e);
      }
    }
    this.dataMap = dataMap;
    Mantle.logger.info("Finished loading {} {} in {} ms", dataMap.size(), name, (System.nanoTime() - time) / 1000000f);
  }

  /** File id paired with its JSON, after parents are resolved */
  private record JsonFile(Identifier location, JsonObject json) {}

  /** Parses an entry after resolving its parents, copying missing keys from them */
  @SuppressWarnings("unused")  // API
  public static <D> D parseData(String name, Map<Identifier, JsonElement> jsons, Identifier location, JsonObject json, @Nullable Map<Identifier, D> locationMap, RecordLoadable<D> dataLoader, TypedMap context) {
    return parseData(name, jsons, location, json, locationMap, dataLoader, context, COPY_PARENT_DATA);
  }

  /**
   * Parses an entry after resolving its parents.
   * WHY 8 parameters: public upstream signature that Tinkers' material render info and texture generator call.
   * @param name         Name for error messages
   * @param jsons        Every loaded file; files with parents are changed in place as they are resolved
   * @param location     Id of the entry being parsed
   * @param json         JSON of the entry
   * @param locationMap  Cache of parsed data per file; null when the result also depends on the context
   * @param dataLoader   Loadable for parsing the value
   * @param context      Parse context
   * @param merger       Copies data from the parent JSON (second) into the child JSON (first)
   */
  public static <D> D parseData(String name, Map<Identifier, JsonElement> jsons, Identifier location, JsonObject json, @Nullable Map<Identifier, D> locationMap, RecordLoadable<D> dataLoader, TypedMap context, BiConsumer<JsonObject, JsonObject> merger) {
    JsonFile resolved = processParents(name, jsons, new ArrayList<>(), location, json, merger);
    if (locationMap == null) {
      return dataLoader.deserialize(resolved.json, context);
    }
    D parsed = locationMap.get(resolved.location);
    if (parsed == null) {
      parsed = dataLoader.deserialize(resolved.json, context);
      locationMap.put(resolved.location, parsed);
    }
    return parsed;
  }

  /**
   * Fetches the parent JSON of a file.
   * @param loadingStack  Files being resolved, for detecting cycles; null skips the check
   * @throws JsonSyntaxException  If the parent is missing, is not an object, or leads back to this file
   */
  public static JsonObject fetchParent(String name, Map<Identifier, JsonElement> jsons, Identifier parentLocation, Identifier location, @Nullable List<Identifier> loadingStack) {
    if (loadingStack != null) {
      loadingStack.add(location);
      if (loadingStack.contains(parentLocation)) {
        throw new JsonSyntaxException("Caught circular dependency trying to resolve " + name + " parent for " + location + ", ignoring parent. Full stack " + loadingStack);
      }
    }
    JsonElement element = jsons.get(parentLocation);
    if (element == null) {
      throw new JsonSyntaxException("Missing parent at " + parentLocation + " for " + name + ", used in " + location);
    }
    return GsonHelper.convertToJsonObject(element, parentLocation.toString());
  }

  /**
   * Resolves the parents of a file.
   * A file holding only a parent key is a redirect: the parent's JSON is used as is, so both share cached data.
   * Any other file gets the parent's data merged in and loses its parent key, so it is resolved once per reload.
   * @return  The JSON to parse, with the id of the file it came from for the caller's cache
   */
  private static JsonFile processParents(String name, Map<Identifier, JsonElement> jsons, List<Identifier> loadingStack, Identifier location, JsonObject json, BiConsumer<JsonObject, JsonObject> merger) {
    while (json.has(PARENT)) {
      Identifier parentLocation = JsonHelper.getIdentifier(json, PARENT);
      JsonObject parentJson = fetchParent(name, jsons, parentLocation, location, loadingStack);
      if (json.keySet().size() > 1) {
        JsonObject resolvedParent = processParents(name, jsons, loadingStack, parentLocation, parentJson, merger).json;
        merger.accept(json, resolvedParent);
        json.remove(PARENT);
        break;
      }
      json = parentJson;
      location = parentLocation;
    }
    return new JsonFile(location, json);
  }

  /** Gets the data of a registry entry, or null when it has none */
  public @Nullable D get(R object) {
    return dataMap.get(object);
  }

  /** Gets the data of a registry entry, or the default when it has none */
  public D get(R object, D defaultValue) {
    return dataMap.getOrDefault(object, defaultValue);
  }
}
