package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.field.AlwaysPresentRecordField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Field whose registry object is stored in the parent object itself, with its type under a different key.
 * For example a {@code "predicate_type"} key next to the parent's own {@code "type"}.
 * @param loadable  Registry loadable reading {@code "type"}
 * @param typeKey   Key holding the nested type in the parent
 * @param getter    Reads the value from the parent
 */
public record MergingRegistryField<T, P>(RecordLoadable<T> loadable, String typeKey, Function<P, T> getter) implements AlwaysPresentRecordField<T, P> {
  /**
   * Copies the object with {@code typeKey} renamed to {@code "type"}.
   * WHY a copy: upstream rewrote the parent JSON in place, which surprises later readers of the same object.
   */
  public static JsonObject mapType(JsonObject json, String typeKey) {
    JsonObject copy = new JsonObject();
    for (Entry<String, JsonElement> entry : json.entrySet()) {
      if (!typeKey.equals(entry.getKey())) {
        copy.add(entry.getKey(), entry.getValue());
      }
    }
    copy.addProperty("type", GsonHelper.getAsString(json, typeKey));
    return copy;
  }

  /** Writes a serialized registry object into the parent, renaming its {@code "type"} to {@code typeKey} */
  public static void serializeInto(JsonObject json, String typeKey, JsonElement element) {
    if (element.isJsonPrimitive()) {
      // a primitive is a compact type id with no other data
      json.add(typeKey, element);
      return;
    }
    if (!element.isJsonObject()) {
      throw new JsonIOException("Unable to serialize nested object, expected string or object");
    }
    for (Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
      String key = entry.getKey();
      if (typeKey.equals(key)) {
        throw new JsonIOException("Unable to serialize nested object, object already has key " + typeKey);
      }
      json.add("type".equals(key) ? typeKey : key, entry.getValue());
    }
  }

  @Override
  public T get(JsonObject json, TypedMap context) {
    return loadable.deserialize(mapType(json, typeKey), context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    serializeInto(json, typeKey, loadable.serialize(getter.apply(parent)));
  }
}
