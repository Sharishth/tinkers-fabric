package slimeknights.mantle.data.loadable.record;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Record loadable for a single fixed instance with no fields.
 * @param <T>  Instance type
 */
@RequiredArgsConstructor
public class SingletonLoader<T> implements RecordLoadable<T> {
  @Getter
  private final T instance;

  /** Creates the instance with this loader, for objects that hold their own loader */
  public SingletonLoader(Function<RecordLoadable<T>, T> creator) {
    this.instance = creator.apply(this);
  }

  /** Creates a singleton instance that holds its own loader */
  public static <T> T singleton(Function<RecordLoadable<T>, T> instance) {
    return new SingletonLoader<>(instance).getInstance();
  }

  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    return instance;
  }

  @Override
  public void serialize(T object, JsonObject json) {}

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return instance;
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T value) {}
}
