package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.LegacyLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Field that also accepts an old key, with a warning, when the current key is missing.
 * Always writes the current key.
 * @param base      Field using the current key
 * @param fallback  Deprecated key
 */
public record LegacyField<T, P>(LoadableField<T, P> base, String fallback) implements LoadableField<T, P> {
  public LegacyField {
    if (base.key().equals(fallback)) {
      throw new IllegalArgumentException("Cannot create a legacy key with a fallback matching the base key '" + base.key() + "'");
    }
  }

  @Override
  public String key() {
    return base.key();
  }

  @Override
  public T get(JsonObject json, String key, TypedMap context) {
    if (json.has(fallback) && !json.has(key)) {
      Mantle.logger.warn("Using deprecated JSON key '{}'{}, switch to current name of '{}'", fallback, LegacyLoadable.whileParsing(context), key);
      return base.get(json, fallback, context);
    }
    // main key present, or both missing: the base field decides how to handle a missing value
    return base.get(json, key, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    base.serialize(parent, json);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return base.decode(buffer, context);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, P parent) {
    base.encode(buffer, parent);
  }
}
