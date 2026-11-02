package slimeknights.mantle.registration.deferred;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import slimeknights.mantle.registration.object.EntityObject;

import java.util.function.Supplier;

/**
 * Deferred register for an entity, building the type from a builder instance and adding an egg
 */
@SuppressWarnings("unused")
public class EntityTypeDeferredRegister extends DeferredRegisterWrapper<EntityType<?>> {

  private final SynchronizedDeferredRegister<Item> itemRegistry;
  public EntityTypeDeferredRegister(String modID) {
    super(Registries.ENTITY_TYPE, modID);
    itemRegistry = SynchronizedDeferredRegister.create(Registries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    itemRegistry.register();
  }

  /**
   * Registers a entity type for the given entity type builder
   * @param name  Entity name
   * @param sup   Entity builder instance
   * @param <T>   Entity class type
   * @return  Entity registry object
   */
  public <T extends Entity> RegistryObject<EntityType<T>> register(String name, Supplier<EntityType.Builder<T>> sup) {
    ResourceKey<EntityType<?>> key = key(Registries.ENTITY_TYPE, name);
    return register.register(name, () -> sup.get().build(key));
  }

  /**
   * Registers a entity type for the given entity type builder, and registers a spawn egg for it.
   * 26.x removed spawn egg colors; the egg look comes from its item model and texture (T-74).
   * @param name  Entity name
   * @param sup   Entity builder instance
   * @param <T>   Entity class type
   * @return  Entity registry object
   */
  public <T extends Mob> EntityObject<T> registerWithEgg(String name, Supplier<EntityType.Builder<T>> sup) {
    RegistryObject<EntityType<T>> object = register(name, sup);
    ResourceKey<Item> eggKey = key(Registries.ITEM, name + "_spawn_egg");
    return new EntityObject<>(object, itemRegistry.register(name + "_spawn_egg", () -> new SpawnEggItem(new Item.Properties().spawnEgg(object.get()).setId(eggKey))));
  }
}
