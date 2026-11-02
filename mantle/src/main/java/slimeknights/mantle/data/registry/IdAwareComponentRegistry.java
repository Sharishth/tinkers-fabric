package slimeknights.mantle.data.registry;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Named component registry for components that know their own id */
public class IdAwareComponentRegistry<T extends IdAwareObject> extends AbstractNamedComponentRegistry<T> {
  private final Map<Identifier, T> values = new HashMap<>();

  /** @param errorText  Error shown before an unknown id */
  public IdAwareComponentRegistry(String errorText) {
    super(errorText);
  }

  /**
   * Registers a component under its own id; each id may only be registered once.
   * @return  The component, for assigning to a constant
   */
  public synchronized <V extends T> V register(V value) {
    Identifier name = value.getId();
    if (values.putIfAbsent(name, value) != null) {
      throw new IllegalArgumentException("Duplicate registration " + name);
    }
    return value;
  }

  @Override
  public @Nullable T getValue(Identifier name) {
    return values.get(name);
  }

  @Override
  public Identifier getKey(T object) {
    return object.getId();
  }

  @Override
  public Collection<Identifier> getKeys() {
    return values.keySet();
  }

  @Override
  public Collection<T> getValues() {
    return values.values();
  }
}
