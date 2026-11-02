package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import slimeknights.mantle.data.JsonCodec;

/**
 * Codec backed by a loadable, through JSON.
 * Slower than a native codec for NBT, as the data is converted to JSON first.
 * @param loadable  Loadable doing the work
 */
public record LoadableCodec<T>(Loadable<T> loadable) implements JsonCodec<T> {
  @Override
  public T deserialize(JsonElement element, DynamicOps<?> ops) {
    return loadable.convert(element, "codec");
  }

  @Override
  public JsonElement serialize(T object, DynamicOps<?> ops) {
    return loadable.serialize(object);
  }
}
