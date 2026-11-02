package slimeknights.mantle.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Gson serializer for ids, where ids without a namespace use the given mod id.
 * Upstream's ResourceLocationSerializer was generic over ResourceLocation subclasses; 26.x's Identifier is final,
 * so id types wrap an Identifier and are built through {@code fromId}. Values are written with {@code toString()}.
 * @param <T>  Id type, written as {@code namespace:path} by its toString
 */
@RequiredArgsConstructor
public class IdentifierSerializer<T> implements JsonDeserializer<T>, JsonSerializer<T> {
  private final Function<Identifier, T> fromId;
  private final String modId;

  /** Creates a serializer for plain identifiers */
  public static IdentifierSerializer<Identifier> identifier(String modId) {
    return new IdentifierSerializer<>(Function.identity(), modId);
  }

  @Override
  public JsonElement serialize(T id, Type type, JsonSerializationContext context) {
    return new JsonPrimitive(id.toString());
  }

  @Override
  public T deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
    String text = GsonHelper.convertToString(element, "location");
    if (text.indexOf(':') < 0) {
      text = modId + ":" + text;
    }
    return fromId.apply(JsonHelper.parseIdentifier(text, "location"));
  }
}
