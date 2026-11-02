package slimeknights.mantle.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

/**
 * JSON reading helpers.
 * Ported so far: identifier parsing (T-13a). The rest of upstream's helpers follow with T-13e.
 */
public final class JsonHelper {
  private JsonHelper() {}

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
}
