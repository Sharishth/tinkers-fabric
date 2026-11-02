package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Field read from JSON on the server but never sent to clients, which get a fixed value instead.
 * @param field        Field doing the JSON work
 * @param clientValue  Value clients get
 */
public record UnsyncedField<T, P>(LoadableField<T, P> field, @Nullable T clientValue) implements LoadableField<T, P> {
  /** Creates a field whose client value is the field's default, or null when it has none */
  public UnsyncedField(LoadableField<T, P> field) {
    this(field, field instanceof DefaultingField<T, P> defaulting ? defaulting.defaultValue() : null);
  }

  @Override
  public String key() {
    return field.key();
  }

  @Override
  public T get(JsonObject json, String key, TypedMap context) {
    return field.get(json, key, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    field.serialize(parent, json);
  }

  @Override
  public @Nullable T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return clientValue;
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, P parent) {}
}
