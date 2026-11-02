package slimeknights.mantle.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.gson.IdentifierSerializer;
import slimeknights.mantle.data.loadable.Loadable;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * JSON reading helpers: identifiers, lists, resource files and codecs.
 * Not ported: packet syncing on datapack reload (T-16), Forge registry lookups (use Loadables), and upstream's
 * deprecated enum and block state helpers (use EnumLoadable and BlockStateLoadable).
 */
@SuppressWarnings("unused")  // API
public final class JsonHelper {
  private JsonHelper() {}

  /** Pretty-printing Gson that reads identifiers, for writing files */
  public static final Gson DEFAULT_GSON = new GsonBuilder()
    .registerTypeAdapter(Identifier.class, IdentifierSerializer.identifier(Identifier.DEFAULT_NAMESPACE))
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create();


  /* Identifiers */

  /**
   * Parses an identifier, with a JSON error naming the key when invalid.
   * Upstream's parseResourceLocation, renamed for 26.x's Identifier.
   * @throws JsonSyntaxException  If the text is not a valid identifier
   */
  public static Identifier parseIdentifier(String text, String key) {
    return Identifier.read(text).getOrThrow(error -> new JsonSyntaxException("Expected " + key + " to be an identifier, was '" + text + "': " + error));
  }

  /** Reads a required identifier field of a JSON object */
  public static Identifier getIdentifier(JsonObject json, String key) {
    return parseIdentifier(GsonHelper.getAsString(json, key), key);
  }

  /** Reads an identifier field of a JSON object, or the fallback when it is missing */
  public static @Nullable Identifier getIdentifier(JsonObject json, String key, @Nullable Identifier fallback) {
    if (json.has(key)) {
      return getIdentifier(json, key);
    }
    return fallback;
  }

  /** Reads an identifier from a JSON element */
  public static Identifier convertToIdentifier(JsonElement json, String key) {
    return parseIdentifier(GsonHelper.convertToString(json, key), key);
  }


  /* Elements and lists */

  /**
   * Gets a member of any type.
   * @throws JsonSyntaxException  If the member is missing
   */
  public static JsonElement getElement(JsonObject json, String memberName) {
    if (!json.has(memberName)) {
      throw new JsonSyntaxException("Missing " + memberName);
    }
    return json.get(memberName);
  }

  /**
   * Parses a non-empty JSON array into an immutable list.
   * @param mapper  Reads one element, given the element and its key such as {@code name[2]}
   * @throws JsonSyntaxException  If the array is empty or an element is invalid
   */
  public static <T> List<T> parseList(JsonArray array, String name, BiFunction<JsonElement, String, T> mapper) {
    if (array.isEmpty()) {
      throw new JsonSyntaxException(name + " must have at least 1 element");
    }
    List<T> builder = new ArrayList<>(array.size());
    for (int i = 0; i < array.size(); i++) {
      builder.add(mapper.apply(array.get(i), name + "[" + i + "]"));
    }
    return List.copyOf(builder);
  }

  /** Parses a non-empty JSON array with a loadable */
  public static <T> List<T> parseList(JsonArray array, String name, Loadable<T> mapper) {
    return parseList(array, name, mapper::convert);
  }

  /** Parses a non-empty JSON array member with a loadable */
  public static <T> List<T> parseList(JsonObject parent, String name, Loadable<T> mapper) {
    return parseList(GsonHelper.getAsJsonArray(parent, name), name, mapper);
  }

  /** Parses a non-empty JSON array member */
  public static <T> List<T> parseList(JsonObject parent, String name, BiFunction<JsonElement, String, T> mapper) {
    return parseList(GsonHelper.getAsJsonArray(parent, name), name, mapper);
  }


  /* Resources */

  /** Reads a resource as a JSON object, or logs and returns null when it is unreadable */
  public static @Nullable JsonObject getJson(Resource resource, Identifier location) {
    try (Reader reader = resource.openAsReader()) {
      return GsonHelper.parse(reader);
    } catch (JsonParseException | IOException e) {
      Mantle.logger.error("Failed to load JSON from resource {} from pack '{}'", location, resource.sourcePackId(), e);
      return null;
    }
  }

  /**
   * Reads the file at the path from every namespace and every pack, lowest priority pack first.
   * @param preferredPath  If set, the path is deprecated and a warning names this one instead
   */
  public static List<JsonObject> getFileInAllDomainsAndPacks(ResourceManager manager, String path, @Nullable String preferredPath) {
    return manager.getNamespaces().stream()
      .filter(Identifier::isValidNamespace)
      .flatMap(namespace -> {
        Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
        return manager.getResourceStack(location).stream().map(resource -> {
          if (preferredPath != null) {
            Mantle.logger.warn("Using deprecated path {} in pack {} - use {}:{} instead", location, resource.sourcePackId(), location.getNamespace(), preferredPath);
          }
          return getJson(resource, location);
        });
      })
      .filter(Objects::nonNull)
      .toList();
  }

  /** Removes the folder prefix and extension suffix from a resource path, such as {@code recipes/x.json} to {@code x} */
  public static String localize(String path, String folder, String extension) {
    return path.substring(folder.length() + 1, path.length() - extension.length());
  }

  /** Removes the folder prefix and extension suffix from an identifier's path */
  public static Identifier localize(Identifier location, String folder, String extension) {
    return location.withPath(localize(location.getPath(), folder, extension));
  }

  /** Adds a prefix and suffix to an identifier's path */
  public static Identifier wrap(Identifier location, String prefix, String suffix) {
    return location.withPath(prefix + location.getPath() + suffix);
  }


  /* Codecs */

  /**
   * Reads JSON text with a codec.
   * @throws JsonParseException  If the text or the value is invalid
   */
  public static <T> T parse(Codec<T> codec, Reader reader) throws JsonParseException {
    return parse(codec, GsonHelper.parse(reader));
  }

  /**
   * Reads a JSON element with a codec; built-in registries only, see LoadableRegistries for datapack ones.
   * @throws JsonParseException  If the value is invalid
   */
  public static <T> T parse(Codec<T> codec, JsonElement json) throws JsonParseException {
    return codec.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
  }

  /**
   * Writes a value to JSON with a codec.
   * @throws IllegalStateException  If the value cannot be written
   */
  public static <T> JsonElement serialize(Codec<T> codec, T object) {
    return codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow(IllegalStateException::new);
  }
}
