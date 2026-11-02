package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Field that is null when missing from JSON, and left out of written JSON when null.
 * On the network a presence flag comes before the value.
 * @param loadable  Loadable for the value
 * @param key       JSON key
 * @param getter    Reads the possibly null value from the parent
 */
public record NullableField<T, P>(Loadable<T> loadable, String key, Function<P, @Nullable T> getter) implements LoadableField<T, P> {
  @Override
  public @Nullable T get(JsonObject json, String key, TypedMap context) {
    return loadable.getOrDefault(json, key, null, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    T object = getter.apply(parent);
    if (object != null) {
      json.add(key, loadable.serialize(object));
    }
  }

  @Override
  public @Nullable T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    if (buffer.readBoolean()) {
      return loadable.decode(buffer, context);
    }
    return null;
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, P parent) {
    T object = getter.apply(parent);
    buffer.writeBoolean(object != null);
    if (object != null) {
      loadable.encode(buffer, object);
    }
  }
}
