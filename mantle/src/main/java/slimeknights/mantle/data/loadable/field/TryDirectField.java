package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Field read from its key when present, otherwise from the parent object itself.
 * Written straight into the parent when its keys cannot be confused with the parent's, otherwise under the key.
 * @param loadable   Loadable for the value; must accept JSON objects
 * @param key        JSON key
 * @param getter     Reads the value from the parent
 * @param conflicts  Extra keys that block writing directly, such as optional sibling fields not written yet
 */
public record TryDirectField<T, P>(Loadable<T> loadable, String key, Function<P, T> getter, String... conflicts) implements AlwaysPresentLoadableField<T, P> {
  @Override
  public T get(JsonObject json, String key, TypedMap context) {
    if (json.has(key)) {
      return loadable.convert(json.get(key), key, context);
    }
    return loadable.convert(json, key, context);
  }

  /** Checks if writing the serialized keys into the parent would make reading ambiguous */
  private boolean hasConflict(JsonObject parent, JsonObject serialized) {
    if (serialized.has(key)) {
      return true;
    }
    for (String existing : parent.keySet()) {
      if (serialized.has(existing)) {
        return true;
      }
    }
    for (String conflict : conflicts) {
      if (serialized.has(conflict)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    JsonElement element = loadable.serialize(getter.apply(parent));
    if (element.isJsonObject() && !hasConflict(json, element.getAsJsonObject())) {
      for (Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
        json.add(entry.getKey(), entry.getValue());
      }
      return;
    }
    json.add(key, element);
  }
}
