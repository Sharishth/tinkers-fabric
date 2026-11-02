package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.field.DefaultingField;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

/**
 * Loader registry with a default instance, used for JSON null, missing fields, and sent as an empty id.
 * @param <T>  Base type
 */
public class DefaultingLoaderRegistry<T extends GenericLoaderRegistry.IHaveLoader> extends GenericLoaderRegistry<T> {
  private final T defaultInstance;

  /** @param falseInstance  Value for a failed load condition */
  public DefaultingLoaderRegistry(String name, T defaultInstance, @Nullable T falseInstance, boolean compact) {
    super(name, falseInstance, compact);
    this.defaultInstance = defaultInstance;
  }

  /** Creates a registry whose default is also the value for failed load conditions */
  @SuppressWarnings("unused")  // API
  public DefaultingLoaderRegistry(String name, T defaultInstance, boolean compact) {
    this(name, defaultInstance, defaultInstance, compact);
  }

  /** Gets the default instance */
  public T getDefault() {
    return defaultInstance;
  }


  /* JSON */

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    if (element.isJsonNull()) {
      return defaultInstance;
    }
    return super.convert(element, key, context);
  }

  /** Gson hook: writes the default as JSON null */
  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    if (src == defaultInstance) {
      return JsonNull.INSTANCE;
    }
    return serialize(src);
  }

  /** Reads the field, or the default when missing */
  public T getOrDefault(JsonObject parent, String key, TypedMap context) {
    return Objects.requireNonNull(super.getOrDefault(parent, key, defaultInstance, context));
  }

  /** Reads the field with empty context, or the default when missing */
  public T getOrDefault(JsonObject parent, String key) {
    return getOrDefault(parent, key, TypedMap.EMPTY);
  }


  /* Network */

  @SuppressWarnings("unchecked")  // only used as a lookup key; registered loaders produce subtypes of T
  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T src) {
    if (src == defaultInstance) {
      loaders.encodeOptional(buffer, null);
      return;
    }
    RecordLoadable<? extends IHaveLoader> loader = src.getLoader();
    loaders.encodeOptional(buffer, (RecordLoadable<? extends T>) loader);
    encode(loader, buffer, src);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    RecordLoadable<? extends T> loader = loaders.decodeOptional(buffer);
    if (loader == null) {
      return defaultInstance;
    }
    return loader.decode(buffer, context);
  }


  /* Fields */

  /** Creates a field that is the default instance when missing */
  public <P> LoadableField<T, P> defaultField(String key, boolean serializeDefault, Function<P, T> getter) {
    return new DefaultingField<>(this, key, defaultInstance, serializeDefault, getter);
  }

  /** Creates a field that is the default instance when missing, and left out of written JSON when default */
  public <P> LoadableField<T, P> defaultField(String key, Function<P, T> getter) {
    return defaultField(key, false, getter);
  }
}
