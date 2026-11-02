package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Predicate;

/**
 * Loadable with a short JSON form for simple values, such as a plain string instead of an object.
 * Reads the short form whenever the JSON is not an object; the network always uses the full form.
 * @param <T>  Value type
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CompactLoadable<T> implements Loadable<T> {
  private final Loadable<T> loadable;
  private final Loadable<T> compact;
  private final Predicate<T> compactCondition;

  /**
   * Creates a compact loadable.
   * @param loadable          Full form
   * @param compact           Short form, read from any non-object JSON
   * @param compactCondition  True when a value should be written in the short form
   */
  public static <T> Loadable<T> of(Loadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
    return new CompactLoadable<>(loadable, compact, compactCondition);
  }

  /** Creates a compact loadable whose full form is a record, keeping the record form */
  public static <T> RecordLoadable<T> of(RecordLoadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
    return new Record<>(loadable, compact, compactCondition);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    if (!element.isJsonObject()) {
      return compact.convert(element, key, context);
    }
    return loadable.convert(element, key, context);
  }

  @Override
  public JsonElement serialize(T object) {
    if (compactCondition.test(object)) {
      return compact.serialize(object);
    }
    return loadable.serialize(object);
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return loadable.decode(buffer, context);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T object) {
    loadable.encode(buffer, object);
  }

  /** Compact loadable with a record full form; convert and serialize(T) stay compact-aware */
  private static class Record<T> extends CompactLoadable<T> implements RecordLoadable<T> {
    private final RecordLoadable<T> loadable;

    Record(RecordLoadable<T> loadable, Loadable<T> compact, Predicate<T> compactCondition) {
      super(loadable, compact, compactCondition);
      this.loadable = loadable;
    }

    @Override
    public T deserialize(JsonObject json, TypedMap context) {
      return loadable.deserialize(json, context);
    }

    @Override
    public void serialize(T object, JsonObject json) {
      loadable.serialize(object, json);
    }
  }
}
