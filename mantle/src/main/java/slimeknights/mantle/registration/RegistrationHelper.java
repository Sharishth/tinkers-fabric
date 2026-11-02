package slimeknights.mantle.registration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * Small registration utilities.
 * Ported: bucket properties and casted holders. Argument type casting moves with commands (T-17).
 * Not ported: registerWoodType/forEachWoodType, since BlockDeferredRegister.registerWood uses Fabric's WoodTypeBuilder
 * and 26.3 sign textures need no atlas list; Forge-only injection and missing-mapping helpers have no Fabric equivalent (I-009).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegistrationHelper {
  /**
   * Applies standard bucket item properties.
   * WHY a method, not upstream's shared BUCKET_PROPS: 26.x properties carry the item id, so they cannot be shared (D-18).
   * Use as {@code ITEMS.register(name, p -> new BucketItem(fluid, RegistrationHelper.bucketProperties(p)))}.
   * @param props  Properties already carrying the item id
   * @return  The same properties, with a bucket crafting remainder and a stack size of 1
   */
  public static Item.Properties bucketProperties(Item.Properties props) {
    return props.craftRemainder(Items.BUCKET).stacksTo(1);
  }

  /**
   * Gets a supplier for an already registered entry
   * @param registry  Registry instance, used to make sure the entry is registered
   * @param entry     Entry to wrap
   * @param <T>       Registry type
   * @param <R>       Return type, typically but not strictly registry type
   * @return  Supplier for the given entry casted to the requested type
   */
  @SuppressWarnings("unchecked")  // we know the entry is the given type
  public static <T, R extends T> Supplier<R> getCastedHolder(Registry<T> registry, T entry) {
    if (registry.getResourceKey(entry).isEmpty()) {
      throw new IllegalArgumentException("Entry " + entry + " is not registered in " + registry.key().identifier());
    }
    return () -> (R) entry;
  }
}
