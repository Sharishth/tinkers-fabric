package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Field that must be present in JSON.
 * @param loadable       Loadable for the value
 * @param key            JSON key
 * @param serializeNull  If true, a value that writes as JSON null is still written
 * @param getter         Reads the value from the parent
 */
public record RequiredField<T, P>(Loadable<T> loadable, String key, boolean serializeNull, Function<P, T> getter) implements AlwaysPresentLoadableField<T, P> {
  @Override
  public T get(JsonObject json, String key, TypedMap context) {
    return loadable.getIfPresent(json, key, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    JsonElement element = loadable.serialize(getter.apply(parent));
    if (serializeNull || !element.isJsonNull()) {
      json.add(key, element);
    }
  }
}
