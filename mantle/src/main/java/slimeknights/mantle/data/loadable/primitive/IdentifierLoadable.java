package slimeknights.mantle.data.loadable.primitive;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * String loadable for values named by an identifier, such as registry entries.
 * Upstream's ResourceLocationLoadable, renamed for 26.x's Identifier.
 * @param <T>  Value type
 */
@SuppressWarnings("unused")  // API
public interface IdentifierLoadable<T> extends StringLoadable<T> {
  /** Plain identifier */
  StringLoadable<Identifier> DEFAULT = new IdentifierLoadable<>() {
    @Override
    public Identifier fromKey(Identifier name, String key, TypedMap context) {
      return name;
    }

    @Override
    public Identifier getKey(Identifier object) {
      return object;
    }

    @Override
    public Identifier decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return buffer.readIdentifier();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Identifier value) {
      buffer.writeIdentifier(value);
    }
  };

  /**
   * Gets the value for an identifier.
   * @param key  JSON key, for error messages
   * @throws com.google.gson.JsonSyntaxException  If nothing has that identifier
   */
  T fromKey(Identifier name, String key, TypedMap context);

  /** Gets the value for an identifier with empty context */
  default T fromKey(Identifier name, String key) {
    return fromKey(name, key, TypedMap.EMPTY);
  }

  /**
   * Gets the identifier of a value.
   * @throws RuntimeException  If the value has no identifier
   */
  Identifier getKey(T object);

  @Override
  default T parseString(String value, String key, TypedMap context) {
    return fromKey(JsonHelper.parseIdentifier(value, key), key, context);
  }

  @Override
  default String getString(T object) {
    return getKey(object).toString();
  }
}
