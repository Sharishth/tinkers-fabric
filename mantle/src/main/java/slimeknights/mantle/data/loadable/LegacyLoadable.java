package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Record loadable that wraps another to accept an old JSON format while reading.
 * Subclasses override {@link #deserialize(JsonObject, TypedMap)}; everything else goes to the base.
 * See {@link slimeknights.mantle.data.loadable.field.LegacyField} for the simpler case of a renamed key.
 */
@RequiredArgsConstructor
public abstract class LegacyLoadable<T> implements RecordLoadable<T> {
  protected final RecordLoadable<T> base;

  /** Gets " while parsing x" from the context's debug info, or an empty string */
  public static String whileParsing(TypedMap context) {
    String debug = context.get(ContextKey.DEBUG);
    return debug != null ? " while parsing " + debug : "";
  }

  /** Wraps a loader to log a deprecation message every time it reads JSON */
  public static <T> RecordLoadable<T> message(RecordLoadable<T> loader, String message) {
    return new DeprecatedLoader<>(loader, message);
  }

  @Override
  public JsonElement serialize(T object) {
    return base.serialize(object);
  }

  @Override
  public void serialize(T object, JsonObject json) {
    base.serialize(object, json);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T value) {
    base.encode(buffer, value);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return base.decode(buffer, context);
  }

  @Override
  public <P> LoadableField<T, P> nullableField(String key, Function<P, @Nullable T> getter) {
    return base.nullableField(key, getter);
  }

  @Override
  public <P> LoadableField<T, P> defaultField(String key, T defaultValue, boolean serializeDefault, Function<P, T> getter) {
    return base.defaultField(key, defaultValue, serializeDefault, getter);
  }

  /** Loader that is deprecated as a whole */
  private static class DeprecatedLoader<T> extends LegacyLoadable<T> {
    private final String message;

    DeprecatedLoader(RecordLoadable<T> base, String message) {
      super(base);
      this.message = message;
    }

    @Override
    public T deserialize(JsonObject json, TypedMap context) {
      Mantle.logger.warn("Encountered Deprecated Loadable{}: {}", whileParsing(context), message);
      return base.deserialize(json, context);
    }
  }
}
