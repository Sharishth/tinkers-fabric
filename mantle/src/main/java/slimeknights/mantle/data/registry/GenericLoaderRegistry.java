package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Loadable for a polymorphic type: JSON {@code "type"} names the loader that reads the rest of the object.
 * Objects name their own loader through {@link IHaveLoader}, which is how writing finds it.
 * @param <T>  Base type
 */
public class GenericLoaderRegistry<T extends GenericLoaderRegistry.IHaveLoader> implements RecordLoadable<T> {
  /** Name of the type, for errors */
  @Getter
  private final String name;
  protected final NamedComponentRegistry<RecordLoadable<? extends T>> loaders;
  /** If true, a loader with no fields may be written and read as just its type id */
  protected final boolean compact;

  /**
   * Creates a registry.
   * @param falseInstance  Value for a failed load condition; unused until load conditions are ported
   * @param compact        If true, a loader with no fields may be written and read as just its type id
   */
  public GenericLoaderRegistry(String name, @Nullable T falseInstance, boolean compact) {
    this.name = name;
    this.compact = compact;
    this.loaders = new NamedComponentRegistry<>("Unknown " + name + " loader");
    // TODO(T-44): register the mantle:load_condition loader (ConditionalLoadable, getConditionalLoader) with falseInstance
  }

  /** Creates a registry without a value for failed load conditions */
  @SuppressWarnings("unused")  // API
  public GenericLoaderRegistry(String name, boolean compact) {
    this(name, null, compact);
  }

  /** Registers a loader under the given type id */
  public void register(Identifier name, RecordLoadable<? extends T> loader) {
    loaders.register(name, loader);
  }

  /** Gets the type id of a loader, or null when unregistered */
  public @Nullable Identifier getName(RecordLoadable<? extends T> loader) {
    return loaders.getOptionalKey(loader);
  }


  /* JSON */

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    if (element.isJsonObject()) {
      return deserialize(element.getAsJsonObject(), context);
    }
    if (compact && element.isJsonPrimitive()) {
      // WHY a new object: upstream reused one static object, which parallel reload listeners could share
      return loaders.convert(element, "type", context).deserialize(new JsonObject(), context);
    }
    throw new JsonSyntaxException("Invalid " + name + " JSON at " + key + ", must be a JSON object" + (compact ? " or a string" : ""));
  }

  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    return loaders.getIfPresent(json, "type", context).deserialize(json, context);
  }

  /** Writes the type and the loader's fields, checking the loader left the type alone */
  @SuppressWarnings("unchecked")  // the object's own loader accepts the object
  private <L> void serialize(RecordLoadable<L> loader, T src, JsonObject json) {
    JsonElement type = new JsonPrimitive(loaders.getKey((RecordLoadable<? extends T>) loader).toString());
    json.add("type", type);
    loader.serialize((L) src, json);
    if (json.get("type") != type) {
      throw new IllegalStateException(name + " serializer " + type.getAsString() + " modified the type key, this is not allowed as it breaks deserialization");
    }
  }

  @Override
  public JsonElement serialize(T src) {
    JsonObject json = new JsonObject();
    serialize(src.getLoader(), src, json);
    if (compact && json.size() == 1) {
      return json.get("type");
    }
    return json;
  }

  @Override
  public void serialize(T object, JsonObject json) {
    serialize(object.getLoader(), object, json);
  }


  /* Network */

  /** Writes the object with its own loader */
  @SuppressWarnings("unchecked")  // the object's own loader accepts the object
  protected <L> void encode(RecordLoadable<L> loader, RegistryFriendlyByteBuf buffer, T src) {
    loader.encode(buffer, (L) src);
  }

  @SuppressWarnings("unchecked")  // only used as a lookup key; registered loaders produce subtypes of T
  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T src) {
    RecordLoadable<? extends IHaveLoader> loader = src.getLoader();
    loaders.encode(buffer, (RecordLoadable<? extends T>) loader);
    encode(loader, buffer, src);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return loaders.decode(buffer, context).decode(buffer, context);
  }


  /* Fields */

  /** Creates a field stored in the parent object, with the type under {@code typeKey} */
  public <P> RecordField<T, P> directField(String typeKey, Function<P, T> getter) {
    return new MergingRegistryField<>(this, typeKey, getter);
  }

  @Override
  public String toString() {
    return getClass().getName() + "('" + name + "')";
  }

  /** Object that names the loader that reads and writes it */
  public interface IHaveLoader {
    /** Gets the loader; it must be registered in the registry reading this object */
    RecordLoadable<? extends IHaveLoader> getLoader();
  }
}
