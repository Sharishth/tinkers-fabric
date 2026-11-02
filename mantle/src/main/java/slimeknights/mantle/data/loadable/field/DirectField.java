package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Field whose record is read from and written into the parent JSON object itself, sharing its keys and context.
 * @param loadable  Record loadable for the value
 * @param getter    Reads the value from the parent
 */
public record DirectField<T, P>(RecordLoadable<T> loadable, Function<P, T> getter) implements AlwaysPresentRecordField<T, P> {
  @Override
  public T get(JsonObject json, TypedMap context) {
    return loadable.deserialize(json, context);
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    loadable.serialize(getter.apply(parent), json);
  }
}
