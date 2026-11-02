package slimeknights.mantle.registration.deferred;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.registration.object.ItemObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks deferred register behaviour that does not need an open registry. */
class DeferredRegisterTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void queuedEntryKnowsIdBeforeRegistration() {
    SynchronizedDeferredRegister<Block> register = SynchronizedDeferredRegister.create(Registries.BLOCK, "mantle_unit");
    RegistryObject<Block> object = register.register("queued", () -> new Block(null));
    assertEquals(Identifier.fromNamespaceAndPath("mantle_unit", "queued"), object.getId());
    assertEquals(Registries.BLOCK, object.getKey().registryKey());
    assertFalse(object.isPresent());
    assertTrue(object.getHolder().isEmpty());
  }

  @Test
  void unboundGetThrowsNullPointerNamingTheId() {
    RegistryObject<Item> object = SynchronizedDeferredRegister.create(Registries.ITEM, "mantle_unit").register("missing", () -> new Item(null));
    // upstream Mantle catches NullPointerException from unbound registry objects, so the type matters
    NullPointerException error = assertThrows(NullPointerException.class, object::get);
    assertTrue(error.getMessage().contains("mantle_unit:missing"), error.getMessage());
  }

  @Test
  void itemObjectGetOrNullIsNullBeforeRegistration() {
    RegistryObject<Item> object = SynchronizedDeferredRegister.create(Registries.ITEM, "mantle_unit").register("later", () -> new Item(null));
    ItemObject<Item> itemObject = new ItemObject<>(object);
    assertNull(itemObject.getOrNull());
    assertEquals(object.getId(), itemObject.getId());
  }

  @Test
  void registryOrderMatchesVanillaRegistrationOrder() {
    Map<Identifier, Integer> order = RegistrationQueue.registryOrder();
    int fluid = RegistrationQueue.orderOf(order, Registries.FLUID);
    int block = RegistrationQueue.orderOf(order, Registries.BLOCK);
    int item = RegistrationQueue.orderOf(order, Registries.ITEM);
    int blockEntity = RegistrationQueue.orderOf(order, Registries.BLOCK_ENTITY_TYPE);
    // Forge fired register events in this order; upstream suppliers rely on it
    assertTrue(fluid < block, "fluids before blocks");
    assertTrue(block < item, "blocks before items");
    assertTrue(item < blockEntity, "items before block entity types");
  }

  @Test
  void unknownRegistrySortsLast() {
    assertEquals(Integer.MAX_VALUE, RegistrationQueue.orderOf(Map.of(), Registries.BLOCK));
  }

  @Test
  void addingAfterRegistrationFails() {
    SynchronizedDeferredRegister<Block> register = SynchronizedDeferredRegister.create(Registries.BLOCK, "mantle_unit");
    // empty register: registering it touches no registry, so this works with frozen vanilla registries
    register.registerEntries();
    IllegalStateException error = assertThrows(IllegalStateException.class, () -> register.register("too_late", () -> new Block(null)));
    assertTrue(error.getMessage().contains("mantle_unit:too_late"), error.getMessage());
  }

  @Test
  void afterFlushTasksRunOnceInOrder() {
    SynchronizedDeferredRegister<Block> register = SynchronizedDeferredRegister.create(Registries.BLOCK, "mantle_unit");
    List<String> calls = new ArrayList<>();
    register.afterFlush(() -> calls.add("first"));
    register.afterFlush(() -> calls.add("second"));
    register.register();
    // only empty registers are ever queued in unit tests, so flushing touches no frozen registry
    RegistrationQueue.flush();
    assertEquals(List.of("first", "second"), calls);
    register.runAfterFlushTasks();
    assertEquals(2, calls.size(), "tasks run once");
  }

  @Test
  void addingTaskAfterRegistrationFails() {
    SynchronizedDeferredRegister<Block> register = SynchronizedDeferredRegister.create(Registries.BLOCK, "mantle_unit");
    register.registerEntries();
    assertThrows(IllegalStateException.class, () -> register.afterFlush(() -> {}));
  }

  @Test
  void registerQueuesOnlyOnce() {
    SynchronizedDeferredRegister<Block> register = SynchronizedDeferredRegister.create(Registries.BLOCK, "mantle_unit");
    int before = RegistrationQueue.pendingCount();
    register.register();
    register.register();
    assertEquals(before + 1, RegistrationQueue.pendingCount());
  }
}
