package slimeknights.mantle.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Gson serializer for a polymorphic type picked by its {@code "type"} key.
 * Older, Gson-only version of {@link slimeknights.mantle.data.registry.GenericLoaderRegistry}; prefer that for new code.
 * @param <T>  Base type
 */
public class GenericRegisteredSerializer<T extends GenericRegisteredSerializer.IJsonSerializable> implements JsonSerializer<T>, JsonDeserializer<T> {
  private final Map<Identifier, JsonDeserializer<? extends T>> deserializers = new HashMap<>();

  /** Registers the deserializer for a type id */
  public void registerDeserializer(Identifier name, JsonDeserializer<? extends T> jsonDeserializer) {
    deserializers.put(name, jsonDeserializer);
  }

  @Override
  public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject object = GsonHelper.convertToJsonObject(json, "transformer");
    Identifier type = JsonHelper.getIdentifier(object, "type");
    JsonDeserializer<? extends T> deserializer = deserializers.get(type);
    if (deserializer == null) {
      throw new JsonSyntaxException("Unknown serializer " + type);
    }
    return deserializer.deserialize(json, typeOfT, context);
  }

  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject serialized = src.serialize(context);
    if (!serialized.has("type")) {
      throw new IllegalArgumentException("Invalid serialized object, missing type");
    }
    String typeText = GsonHelper.getAsString(serialized, "type");
    Identifier type = Identifier.tryParse(typeText);
    if (type == null) {
      throw new IllegalArgumentException("Invalid object type '" + typeText + '\'');
    }
    if (!deserializers.containsKey(type)) {
      throw new IllegalArgumentException("Unregistered object " + typeText);
    }
    return serialized;
  }

  /** Object that writes itself to JSON, including its {@code "type"} */
  public interface IJsonSerializable {
    /** Writes this object, including its type id */
    JsonObject serialize(JsonSerializationContext context);
  }
}
