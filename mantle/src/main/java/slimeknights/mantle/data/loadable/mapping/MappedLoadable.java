package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.BiFunction;

/**
 * Loadable that converts another loadable's values to a new type and back.
 * Create through {@link Loadable#mapChecked(BiFunction, BiFunction)} and its overloads, which keep record and string forms.
 * @param <F>  Base type
 * @param <T>  New type
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MappedLoadable<F, T> implements Loadable<T> {
  private final Loadable<F> base;
  /** Converts base values to the new type */
  protected final BiFunction<F, ErrorFactory, T> from;
  /** Converts new values back to the base type */
  protected final BiFunction<T, ErrorFactory, F> to;

  /** Maps any loadable */
  public static <T, F> Loadable<T> of(Loadable<F> base, BiFunction<F, ErrorFactory, T> constructor, BiFunction<T, ErrorFactory, F> getter) {
    return new MappedLoadable<>(base, constructor, getter);
  }

  /** Maps a record loadable, keeping its record form */
  public static <T, F> RecordLoadable<T> of(RecordLoadable<F> base, BiFunction<F, ErrorFactory, T> constructor, BiFunction<T, ErrorFactory, F> getter) {
    return new Record<>(base, constructor, getter);
  }

  /** Maps a string loadable, keeping its string form */
  public static <T, F> StringLoadable<T> of(StringLoadable<F> base, BiFunction<F, ErrorFactory, T> constructor, BiFunction<T, ErrorFactory, F> getter) {
    return new StringMapped<>(base, constructor, getter);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return from.apply(base.convert(element, key, context), ErrorFactory.JSON_SYNTAX_ERROR);
  }

  @Override
  public JsonElement serialize(T object) {
    return base.serialize(to.apply(object, ErrorFactory.RUNTIME));
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return from.apply(base.decode(buffer, context), ErrorFactory.DECODER_EXCEPTION);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T object) {
    base.encode(buffer, to.apply(object, ErrorFactory.ENCODER_EXCEPTION));
  }

  /** Mapped record loadable */
  private static class Record<F, T> extends MappedLoadable<F, T> implements RecordLoadable<T> {
    private final RecordLoadable<F> base;

    Record(RecordLoadable<F> base, BiFunction<F, ErrorFactory, T> constructor, BiFunction<T, ErrorFactory, F> getter) {
      super(base, constructor, getter);
      this.base = base;
    }

    @Override
    public T deserialize(JsonObject json, TypedMap context) {
      return from.apply(base.deserialize(json, context), ErrorFactory.JSON_SYNTAX_ERROR);
    }

    // convert and serialize(T) stay MappedLoadable's, so a compact base still reads non-object JSON
    @Override
    public void serialize(T object, JsonObject json) {
      base.serialize(to.apply(object, ErrorFactory.RUNTIME), json);
    }
  }

  /** Mapped string loadable */
  private static class StringMapped<F, T> extends MappedLoadable<F, T> implements StringLoadable<T> {
    private final StringLoadable<F> base;

    StringMapped(StringLoadable<F> base, BiFunction<F, ErrorFactory, T> constructor, BiFunction<T, ErrorFactory, F> getter) {
      super(base, constructor, getter);
      this.base = base;
    }

    @Override
    public T parseString(String value, String key, TypedMap context) {
      return from.apply(base.parseString(value, key, context), ErrorFactory.JSON_SYNTAX_ERROR);
    }

    @Override
    public String getString(T object) {
      return base.getString(to.apply(object, ErrorFactory.RUNTIME));
    }
  }
}
