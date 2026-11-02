package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Field that is never stored and always gives a fixed value, which may differ between JSON and the network.
 * @param fromJson    Value given while reading JSON
 * @param fromBuffer  Value given while reading the network
 */
public record ConstantField<T>(T fromJson, T fromBuffer) implements RecordField<T, Object> {
  /** Creates a field giving the same value everywhere */
  public ConstantField(T value) {
    this(value, value);
  }

  @Override
  public T get(JsonObject json, TypedMap context) {
    return fromJson;
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return fromBuffer;
  }

  @Override
  public void serialize(Object parent, JsonObject json) {}

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Object parent) {}
}
