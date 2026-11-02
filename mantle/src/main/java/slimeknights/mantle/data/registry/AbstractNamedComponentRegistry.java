package slimeknights.mantle.data.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Collection;
import java.util.function.Function;

/**
 * Simple registry of named components, such as loaders or fixed instances, read and written by id.
 * Unlike vanilla registries it never freezes, and the network sends ids rather than numbers.
 * @param <T>  Component type
 */
public abstract class AbstractNamedComponentRegistry<T> implements IdentifierLoadable<T> {
  /** Error text, followed by the unknown id */
  protected final String errorText;

  protected AbstractNamedComponentRegistry(String errorText) {
    this.errorText = errorText + " ";
  }

  /** Gets the component with the id, or null */
  public abstract @Nullable T getValue(Identifier name);

  /** Gets every registered id */
  public abstract Collection<Identifier> getKeys();

  /** Gets every registered component */
  public abstract Collection<T> getValues();

  @Override
  public T fromKey(Identifier name, String key, TypedMap context) {
    T value = getValue(name);
    if (value != null) {
      return value;
    }
    throw new JsonSyntaxException(errorText + name + " at '" + key + '\'');
  }


  /* Network */

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T value) {
    buffer.writeIdentifier(getKey(value));
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return decodeInternal(buffer.readIdentifier());
  }

  /** Writes a possibly null component; an empty string means null, as it is never a valid id */
  public void encodeOptional(RegistryFriendlyByteBuf buffer, @Nullable T value) {
    buffer.writeUtf(value != null ? getKey(value).toString() : "");
  }

  /** Reads a component written by {@link #encodeOptional} */
  public @Nullable T decodeOptional(RegistryFriendlyByteBuf buffer) {
    String key = buffer.readUtf(Short.MAX_VALUE);
    if (key.isEmpty()) {
      return null;
    }
    Identifier name = Identifier.tryParse(key);
    if (name == null) {
      throw new DecoderException(errorText + key);
    }
    return decodeInternal(name);
  }

  /** Gets the component for a network id, throwing a decoder error when unknown */
  private T decodeInternal(Identifier name) {
    T value = getValue(name);
    if (value == null) {
      throw new DecoderException(errorText + name);
    }
    return value;
  }


  /* Fields */

  /** WHY its own field: the network folds null into the id string, saving the presence flag */
  @Override
  public <P> LoadableField<T, P> nullableField(String key, Function<P, @Nullable T> getter) {
    return new NullableField<>(this, key, getter);
  }

  /** Nullable component field using an empty id for null on the network */
  private record NullableField<T, P>(AbstractNamedComponentRegistry<T> registry, String key, Function<P, @Nullable T> getter) implements LoadableField<T, P> {
    @Override
    public @Nullable T get(JsonObject json, String key, TypedMap context) {
      return registry.getOrDefault(json, key, null, context);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
      T object = getter.apply(parent);
      if (object != null) {
        json.add(key, registry.serialize(object));
      }
    }

    @Override
    public @Nullable T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return registry.decodeOptional(buffer);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, P parent) {
      registry.encodeOptional(buffer, getter.apply(parent));
    }
  }
}
