package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Handle to an entry queued in a {@link SynchronizedDeferredRegister}.
 * Knows its id immediately; the value becomes available once {@link RegistrationQueue#flush()} registers it.
 * Fabric replacement for Forge's RegistryObject.
 * @param <T>  Entry type
 */
public final class RegistryObject<T> implements Supplier<T>, IdAwareObject {
  private final ResourceKey<T> key;
  @Nullable
  private T value;
  @Nullable
  private Holder<T> holder;

  RegistryObject(ResourceKey<T> key) {
    this.key = key;
  }

  /** Creates an already bound object, for wrapping entries that are registered elsewhere */
  public static <T> RegistryObject<T> of(ResourceKey<T> key, T value, Holder<T> holder) {
    RegistryObject<T> object = new RegistryObject<>(key);
    object.bind(value, holder);
    return object;
  }

  /** Called by the register once the entry is in the registry */
  void bind(T value, Holder<T> holder) {
    this.value = value;
    this.holder = holder;
  }

  @Override
  public Identifier getId() {
    return key.identifier();
  }

  /** Gets the registry key of this entry */
  public ResourceKey<T> getKey() {
    return key;
  }

  /** Checks if the entry has been registered */
  public boolean isPresent() {
    return value != null;
  }

  /**
   * Gets the registered value.
   * @throws NullPointerException  if the entry is not registered yet
   */
  @Override
  public T get() {
    // WHY: NullPointerException, as Forge's RegistryObject threw; upstream Mantle catches exactly that in getOrNull/forEach
    if (value == null) {
      throw new NullPointerException("Registry object not present: " + key.identifier());
    }
    return value;
  }

  /** Gets the registry holder, empty until registered */
  public Optional<Holder<T>> getHolder() {
    return Optional.ofNullable(holder);
  }

  @Override
  public String toString() {
    return "RegistryObject{" + key.identifier() + (isPresent() ? "" : ", unbound") + "}";
  }
}
