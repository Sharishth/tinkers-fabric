package slimeknights.mantle.data.loadable.field;

import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.Function;

/**
 * Record field whose value always exists, so the network form is just the value.
 * @param <T>  Field value type
 * @param <P>  Parent object type
 */
public interface AlwaysPresentRecordField<T, P> extends RecordField<T, P> {
  /** Loadable for the value */
  Loadable<T> loadable();

  /** Reads the value from the parent */
  Function<P, T> getter();

  @Override
  default T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return loadable().decode(buffer, context);
  }

  @Override
  default void encode(RegistryFriendlyByteBuf buffer, P parent) {
    loadable().encode(buffer, getter().apply(parent));
  }
}
