package slimeknights.mantle.data.loadable.primitive;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.mapping.EnumMapLoadable;
import slimeknights.mantle.data.loadable.mapping.EnumSetLoadable;
import slimeknights.mantle.data.loadable.mapping.SetLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Loadable for an enum value, stored as its lowercase name.
 * @param enumClass      Enum class
 * @param allowedValues  Values JSON may use; the network accepts any value of the enum
 */
public record EnumLoadable<E extends Enum<E>>(Class<E> enumClass, E[] allowedValues) implements StringLoadable<E> {
  /** Creates a loadable allowing every value */
  public EnumLoadable(Class<E> enumClass) {
    this(enumClass, enumClass.getEnumConstants());
  }

  /** Creates a loadable allowing only the given values */
  @SafeVarargs
  public static <E extends Enum<E>> EnumLoadable<E> of(E... values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least 1 value for an enum loadable");
    }
    return new EnumLoadable<>(values[0].getDeclaringClass(), values);
  }

  @Override
  public E parseString(String name, String key, TypedMap context) {
    for (E value : allowedValues) {
      if (getString(value).equals(name)) {
        return value;
      }
    }
    throw new JsonSyntaxException("Invalid " + enumClass.getSimpleName() + " " + name);
  }

  @Override
  public String getString(E object) {
    return object.name().toLowerCase(Locale.ROOT);
  }

  // WHY ordinals by hand: 26.3 deprecates FriendlyByteBuf.readEnum/writeEnum, which sent the same var int ordinal
  @Override
  public E decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int index = buffer.readVarInt();
    E[] values = enumClass.getEnumConstants();
    if (index < 0 || index >= values.length) {
      throw new DecoderException("Invalid " + enumClass.getSimpleName() + " ordinal " + index);
    }
    return values[index];
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, E object) {
    buffer.writeVarInt(object.ordinal());
  }

  @Override
  public SetLoadable<E> set(int minSize) {
    return new EnumSetLoadable<>(this, minSize);
  }

  @Override
  public <V> Loadable<Map<E, V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new EnumMapLoadable<>(this, valueLoadable, minSize);
  }

  /** WHY its own field: the network form folds null into the ordinal, saving the presence flag */
  @Override
  public <P> LoadableField<E, P> nullableField(String key, Function<P, @Nullable E> getter) {
    return new NullableEnumField<>(this, key, getter);
  }

  /** Nullable enum field sent as one var int, where the value count means null */
  private record NullableEnumField<E extends Enum<E>, P>(EnumLoadable<E> loadable, String key, Function<P, @Nullable E> getter) implements LoadableField<E, P> {
    @Override
    public @Nullable E get(JsonObject json, String key, TypedMap context) {
      return loadable.getOrDefault(json, key, null, context);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
      E value = getter.apply(parent);
      if (value != null) {
        json.add(key, loadable.serialize(value));
      }
    }

    @Override
    public @Nullable E decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      int index = buffer.readVarInt();
      E[] values = loadable.enumClass.getEnumConstants();
      if (index == values.length) {
        return null;
      }
      if (index < 0 || index > values.length) {
        throw new DecoderException("Invalid " + loadable.enumClass.getSimpleName() + " ordinal " + index);
      }
      return values[index];
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, P parent) {
      E value = getter.apply(parent);
      buffer.writeVarInt(value == null ? loadable.enumClass.getEnumConstants().length : value.ordinal());
    }
  }
}
