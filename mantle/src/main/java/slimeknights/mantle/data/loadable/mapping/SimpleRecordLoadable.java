package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Record loadable with a single key, so a plain value can also be written as {@code {"key": value}}.
 * Non-object JSON is read as the plain value.
 * @param loadable      Loadable for the value
 * @param key           Key in the object form
 * @param defaultValue  Value for an object without the key; null makes the key required
 * @param compact       If true, written as the plain value instead of the object form
 */
public record SimpleRecordLoadable<T>(Loadable<T> loadable, String key, @Nullable T defaultValue, boolean compact) implements RecordLoadable<T> {
  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    if (!element.isJsonObject()) {
      return loadable.convert(element, key, context);
    }
    return RecordLoadable.super.convert(element, key, context);
  }

  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    if (defaultValue != null) {
      // non-null default, so the result is never null
      T value = loadable.getOrDefault(json, key, defaultValue, context);
      return value != null ? value : defaultValue;
    }
    return loadable.getIfPresent(json, key, context);
  }

  @Override
  public JsonElement serialize(T object) {
    if (compact) {
      return loadable.serialize(object);
    }
    return RecordLoadable.super.serialize(object);
  }

  @Override
  public void serialize(T object, JsonObject json) {
    json.add(key, loadable.serialize(object));
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return loadable.decode(buffer, context);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T value) {
    loadable.encode(buffer, value);
  }
}
