package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Field taken from the parse context instead of being stored.
 * @param <T>  Field value type
 */
public interface ContextField<T> extends RecordField<T, Object> {
  /**
   * Gets the value from the context.
   * @param error  Creates the exception when a required value is missing
   * @return  Context value, or null when the field allows it
   */
  @Nullable T get(TypedMap context, ErrorFactory error);

  @Override
  default @Nullable T get(JsonObject json, TypedMap context) {
    return get(context, ErrorFactory.JSON_SYNTAX_ERROR);
  }

  @Override
  default void serialize(Object parent, JsonObject json) {}

  @Override
  default @Nullable T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return get(context, ErrorFactory.DECODER_EXCEPTION);
  }

  @Override
  default void encode(RegistryFriendlyByteBuf buffer, Object parent) {}
}
