package slimeknights.mantle.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import slimeknights.mantle.Mantle;

/**
 * Codec that converts through JSON.
 * Lets JSON-first serializers such as loadables be used wherever vanilla wants a {@link Codec};
 * other formats such as NBT are converted to JSON first, so this is slower than a native codec there.
 * @param <T>  Object type
 */
public interface JsonCodec<T> extends Codec<T> {
  /** Reads the object from JSON; throws {@link JsonParseException} when invalid */
  T deserialize(JsonElement element, DynamicOps<?> ops);

  /** Writes the object to JSON; throws {@link JsonParseException} when it cannot be written */
  JsonElement serialize(T object, DynamicOps<?> ops);

  /** Name of the parsed type, shown in codec errors */
  default String codecError() {
    return toString();
  }

  @Override
  default <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
    try {
      return DataResult.success(Pair.of(deserialize(ops.convertTo(JsonOps.INSTANCE, input), ops), input));
    } catch (JsonParseException e) {
      Mantle.logger.warn("Unable to decode {}", codecError(), e);
      return DataResult.error(e::getMessage);
    }
  }

  @Override
  default <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
    try {
      return DataResult.success(JsonOps.INSTANCE.convertTo(ops, serialize(input, ops)));
    } catch (JsonParseException e) {
      Mantle.logger.warn("Unable to encode {}", codecError(), e);
      return DataResult.error(e::getMessage);
    }
  }

  /**
   * Codec using an existing Gson serializer.
   * @param name       Type name for codec errors
   * @param gson       Gson instance with the serializer registered
   * @param classType  Class to read and write
   */
  record GsonCodec<T>(String name, Gson gson, Class<T> classType) implements JsonCodec<T> {
    @Override
    public T deserialize(JsonElement element, DynamicOps<?> ops) {
      return gson.fromJson(element, classType);
    }

    @Override
    public JsonElement serialize(T object, DynamicOps<?> ops) {
      return gson.toJsonTree(object, classType);
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
