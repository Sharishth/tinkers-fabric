package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.LegacyLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Field that logs a JSON error and uses a fallback value instead of failing the whole object.
 * Network errors are not caught: the server already parsed the value.
 * @param nested        Field doing the work
 * @param valueOnError  Value used when the JSON is invalid
 */
public record CatchErrorsField<T, P>(LoadableField<T, P> nested, @Nullable T valueOnError) implements LoadableField<T, P> {
  @Override
  public String key() {
    return nested.key();
  }

  @Override
  public @Nullable T get(JsonObject json, String key, TypedMap context) {
    try {
      return nested.get(json, key, context);
    } catch (JsonParseException e) {
      Mantle.logger.error("Caught error on field {}{}, substituting fallback value {}.", key(), LegacyLoadable.whileParsing(context), valueOnError, e);
      return valueOnError;
    }
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    nested.serialize(parent, json);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return nested.decode(buffer, context);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, P parent) {
    nested.encode(buffer, parent);
  }
}
