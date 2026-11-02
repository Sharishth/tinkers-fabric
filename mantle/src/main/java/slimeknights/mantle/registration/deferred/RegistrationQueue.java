package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers all queued deferred registers, ordered like Forge's register events.
 * WHY: Forge fired one register event per registry in vanilla registry order (fluids, blocks, items, ...),
 * so upstream code may call {@code block.get()} inside an item supplier. Fabric has no such events,
 * so each mod calls {@link #flush()} at the end of its initializer instead (D-18).
 */
public final class RegistrationQueue {
  private static final List<SynchronizedDeferredRegister<?>> PENDING = new ArrayList<>();

  private RegistrationQueue() {}

  /** Adds a register; called by {@link SynchronizedDeferredRegister#register()} */
  static synchronized void enqueue(SynchronizedDeferredRegister<?> register) {
    PENDING.add(register);
  }

  /**
   * Registers every queued register, registries in vanilla order, entries in the order they were added.
   * Call once at the end of a mod initializer, after all registers called {@code register()}.
   */
  public static synchronized void flush() {
    Map<Identifier, Integer> order = registryOrder();
    List<SynchronizedDeferredRegister<?>> registers = new ArrayList<>(PENDING);
    PENDING.clear();
    // List.sort is stable: registers for the same registry keep the order they were queued in
    registers.sort(Comparator.comparingInt(register -> orderOf(order, register.getRegistryKey())));
    for (SynchronizedDeferredRegister<?> register : registers) {
      register.registerEntries();
    }
  }

  /** Gets the number of registers waiting for {@link #flush()} */
  public static synchronized int pendingCount() {
    return PENDING.size();
  }

  /** Position of each built-in registry in vanilla's registration order */
  static Map<Identifier, Integer> registryOrder() {
    Map<Identifier, Integer> order = new HashMap<>();
    int index = 0;
    for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
      order.put(registry.key().identifier(), index);
      index++;
    }
    return order;
  }

  /** Unknown registries sort last, keeping their queue order */
  static int orderOf(Map<Identifier, Integer> order, ResourceKey<? extends Registry<?>> key) {
    return order.getOrDefault(key.identifier(), Integer.MAX_VALUE);
  }
}
