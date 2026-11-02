package slimeknights.mantle.data.loadable.field;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMap.Key;

import java.util.function.BiFunction;

/**
 * Key for a value in the parse context; also makes record fields that read it.
 * @param <T>  Value type
 */
@RequiredArgsConstructor
public class ContextKey<T> implements Key<T> {
  /** Id of the object being parsed, such as a recipe id */
  public static final ContextKey<Identifier> ID = new ContextKey<>("id");
  /** Extra text for log messages, such as the file being parsed */
  public static final ContextKey<String> DEBUG = new ContextKey<>("debug info");
  // TODO(T-44): CONDITION_CONTEXT key for ConditionalLoadable (upstream Forge ICondition.IContext; Fabric ResourceConditions)

  /** Name of the key, for debug */
  @Getter
  private final String name;

  /** Cached field requiring this key */
  private @Nullable RecordField<T, Object> requiredField;
  /** Cached field that is null when this key is missing */
  private @Nullable RecordField<T, Object> nullableField;

  @Override
  public String toString() {
    return "ContextKey('" + name + "')";
  }

  /** Gets a field that requires this key in the context */
  public RecordField<T, Object> requiredField() {
    if (requiredField == null) {
      requiredField = new Required<>(this, (value, error) -> value);
    }
    return requiredField;
  }

  /** Gets a field that requires this key and maps its value */
  public <M> RecordField<M, Object> mappedField(BiFunction<T, ErrorFactory, M> mapper) {
    return new Required<>(this, mapper);
  }

  /** Gets a field that is null when this key is missing */
  public RecordField<T, Object> nullableField() {
    if (nullableField == null) {
      nullableField = new Defaulting<>(this, null);
    }
    return nullableField;
  }

  /** Gets a field that uses the default when this key is missing */
  public RecordField<T, Object> defaultField(T defaultValue) {
    return new Defaulting<>(this, defaultValue);
  }

  /** Field that throws when the key is missing */
  private record Required<T, M>(ContextKey<T> key, BiFunction<T, ErrorFactory, M> mapper) implements ContextField<M> {
    @Override
    public M get(TypedMap context, ErrorFactory error) {
      T value = context.get(key);
      if (value != null) {
        return mapper.apply(value, error);
      }
      throw error.create("Unable to fetch " + key.name + " from context, this usually implements a broken JSON deserializer");
    }
  }

  /** Field that uses a default when the key is missing */
  private record Defaulting<T>(ContextKey<T> key, @Nullable T defaultValue) implements ContextField<T> {
    @Override
    public @Nullable T get(TypedMap context, ErrorFactory error) {
      return context.getOrDefault(key, defaultValue);
    }
  }
}
