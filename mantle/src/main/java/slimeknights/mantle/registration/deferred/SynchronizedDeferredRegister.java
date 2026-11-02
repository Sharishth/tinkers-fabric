package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Queues entries for one built-in registry and registers them when {@link RegistrationQueue#flush()} runs.
 * Replaces Forge's DeferredRegister: entries are created lazily, in the order Forge's register events used.
 * @param <T>  Registry type
 */
public class SynchronizedDeferredRegister<T> {
  private final ResourceKey<? extends Registry<T>> registryKey;
  private final String modId;
  private final List<Entry<T, ?>> entries = new ArrayList<>();
  /** True once {@link #register()} queued this register */
  private boolean queued = false;
  /** True once the entries are in the registry; later additions would be silently lost */
  private boolean flushed = false;

  protected SynchronizedDeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String modId) {
    this.registryKey = registryKey;
    this.modId = modId;
  }

  /** Creates a new register for the given registry and namespace */
  public static <T> SynchronizedDeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modId) {
    return new SynchronizedDeferredRegister<>(key, modId);
  }

  /**
   * Queues an entry; the supplier runs during {@link RegistrationQueue#flush()}.
   * @param name  Path of the entry id, namespace is the mod id
   * @param sup   Creates the entry
   * @return  Handle that resolves once registered
   */
  public synchronized <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> sup) {
    if (flushed) {
      throw new IllegalStateException("Cannot add " + modId + ":" + name + " to " + registryKey.identifier() + " after it was registered");
    }
    ResourceKey<T> key = ResourceKey.create(registryKey, Identifier.fromNamespaceAndPath(modId, name));
    Entry<T, I> entry = new Entry<>(key, sup);
    entries.add(entry);
    return entry.object;
  }

  /** Queues this register for {@link RegistrationQueue#flush()}; Fabric replacement for register(IEventBus) */
  public synchronized void register() {
    if (!queued) {
      queued = true;
      RegistrationQueue.enqueue(this);
    }
  }

  /** Gets the key of the registry this register targets */
  public ResourceKey<? extends Registry<T>> getRegistryKey() {
    return registryKey;
  }

  /** Gets the namespace used for entry ids */
  public String getNamespace() {
    return modId;
  }

  /** Registers every queued entry, in the order they were added */
  synchronized void registerEntries() {
    Registry<T> registry = lookupRegistry();
    for (Entry<T, ?> entry : entries) {
      entry.registerInto(registry);
    }
    flushed = true;
  }

  @SuppressWarnings("unchecked")
  private Registry<T> lookupRegistry() {
    Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
    if (registry == null) {
      throw new IllegalStateException("No built-in registry " + registryKey.identifier() + " for " + modId + " entries");
    }
    return (Registry<T>) registry;
  }

  /** One queued entry and its handle */
  private static final class Entry<T, I extends T> {
    private final ResourceKey<T> key;
    private final Supplier<? extends I> supplier;
    private final RegistryObject<I> object;

    @SuppressWarnings("unchecked")
    private Entry(ResourceKey<T> key, Supplier<? extends I> supplier) {
      this.key = key;
      this.supplier = supplier;
      // WHY: the handle shares the key; I is a subtype of T so the cast only narrows the type parameter
      this.object = new RegistryObject<>((ResourceKey<I>) (ResourceKey<?>) key);
    }

    @SuppressWarnings("unchecked")
    private void registerInto(Registry<T> registry) {
      I value;
      try {
        value = supplier.get();
      } catch (RuntimeException e) {
        throw new IllegalStateException("Failed to create registry entry " + key.identifier(), e);
      }
      if (value == null) {
        throw new IllegalStateException("Supplier returned null for registry entry " + key.identifier());
      }
      Holder.Reference<I> holder = Registry.registerForHolder(registry, key, value);
      // WHY: vanilla Items.registerItem fills Item.BY_BLOCK for block items; without it Block.asItem() returns air
      if (value instanceof BlockItem blockItem) {
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
      }
      object.bind(value, (Holder<I>) (Holder<?>) holder);
    }
  }
}
