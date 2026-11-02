package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonElement;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Set loadable for enum values, backed by an {@link EnumSet} */
public class EnumSetLoadable<T extends Enum<T>> extends SetLoadable<T> {
  private final Class<T> enumClass;

  public EnumSetLoadable(Class<T> enumClass, Loadable<T> base, int minSize) {
    super(base, minSize);
    this.enumClass = enumClass;
  }

  public EnumSetLoadable(EnumLoadable<T> base, int minSize) {
    this(base.enumClass(), base, minSize);
  }

  @Override
  protected Collection<T> createBuilder(int size) {
    return EnumSet.noneOf(enumClass);
  }

  @SuppressWarnings("unchecked")  // every builder comes from createBuilder or convertCompact, both EnumSets
  @Override
  protected Set<T> build(Collection<T> builder) {
    return Collections.unmodifiableSet((Set<T>) builder);
  }

  /** WHY override: the base builds compact values with List.of, which is not a set */
  @Override
  public Set<T> convertCompact(JsonElement element, String key, TypedMap context) {
    return build(EnumSet.of(base.convert(element, key, context)));
  }
}
