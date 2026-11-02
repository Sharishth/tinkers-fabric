package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loadable for an immutable collection, stored as a JSON array.
 * @param <T>  Element type
 * @param <C>  Collection type
 */
@SuppressWarnings("unused")  // API
@RequiredArgsConstructor
public abstract class CollectionLoadable<T, C extends Collection<T>> implements ArrayLoadable<C> {
  /** Loadable for each element */
  protected final Loadable<T> base;
  /** Fewest elements, or a compact marker from {@link ArrayLoadable} */
  private final int minSize;

  /** Creates the mutable collection elements are read into */
  protected Collection<T> createBuilder(int size) {
    return new ArrayList<>(size);
  }

  /** Turns the filled builder into the final collection */
  protected abstract C build(Collection<T> builder);

  @Override
  public boolean allowCompact() {
    return minSize < 0;
  }

  @Override
  public void checkSize(String key, int size, ErrorFactory error) {
    int effectiveMin = ArrayLoadable.SizeRange.effectiveMinSize(minSize);
    if (size < effectiveMin) {
      throw error.create(key + " must have at least " + effectiveMin + " elements");
    }
  }

  @Override
  public int getLength(C array) {
    return array.size();
  }

  @Override
  public C convertCompact(JsonElement element, String key, TypedMap context) {
    return build(List.of(base.convert(element, key, context)));
  }

  @Override
  public C convertArray(JsonArray array, String key, TypedMap context) {
    Collection<T> builder = createBuilder(array.size());
    for (int i = 0; i < array.size(); i++) {
      builder.add(base.convert(array.get(i), key + '[' + i + ']', context));
    }
    return build(builder);
  }

  @Override
  public JsonElement serializeFirst(C collection) {
    return base.serialize(collection.iterator().next());
  }

  @Override
  public void serializeAll(JsonArray array, C collection) {
    for (T element : collection) {
      array.add(base.serialize(element));
    }
  }

  @Override
  public C decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    int size = buffer.readVarInt();
    Collection<T> builder = createBuilder(size);
    for (int i = 0; i < size; i++) {
      builder.add(base.decode(buffer, context));
    }
    return build(builder);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, C collection) {
    buffer.writeVarInt(collection.size());
    for (T element : collection) {
      base.encode(buffer, element);
    }
  }
}
