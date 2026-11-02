package slimeknights.mantle.data.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/** Named component registry where each component is registered under an explicit id */
public class NamedComponentRegistry<T> extends AbstractNamedComponentRegistry<T> {
  private final BiMap<Identifier, T> values = HashBiMap.create();

  /** @param errorText  Error shown before an unknown id, such as "Unknown modifier loader" */
  public NamedComponentRegistry(String errorText) {
    super(errorText);
  }

  /**
   * Registers a component; each id and each component may only be registered once.
   * @return  The component, for assigning to a constant
   */
  public synchronized <V extends T> V register(Identifier name, V value) {
    if (values.containsKey(name)) {
      throw new IllegalArgumentException("Duplicate registration " + name);
    }
    if (values.containsValue(value)) {
      throw new IllegalArgumentException("Duplicate registration of " + value + " as " + name + ", already registered as " + values.inverse().get(value));
    }
    values.put(name, value);
    return value;
  }

  @Override
  public @Nullable T getValue(Identifier name) {
    return values.get(name);
  }

  /** Gets the id of a component, or null when unregistered */
  public @Nullable Identifier getOptionalKey(T value) {
    return values.inverse().get(value);
  }

  @Override
  public Identifier getKey(T value) {
    Identifier key = getOptionalKey(value);
    if (key == null) {
      throw new IllegalStateException(errorText + value);
    }
    return key;
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
