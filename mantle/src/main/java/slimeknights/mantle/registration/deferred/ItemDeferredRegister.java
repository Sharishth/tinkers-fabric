package slimeknights.mantle.registration.deferred;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Deferred register that registers items with wrappers.
 * WHY factories: 26.x items need their id in Item.Properties before construction, so the register
 * creates the properties with the id set and passes them to the factory (D-18).
 */
@SuppressWarnings("unused")
public class ItemDeferredRegister extends DeferredRegisterWrapper<Item> {

  public ItemDeferredRegister(String modID) {
    super(Registries.ITEM, modID);
  }

  /**
   * Adds a new item to the list to be registered.
   * @param name     Item name
   * @param props    Creates the base properties, called once per item; the id is set afterwards
   * @param factory  Creates the item from properties that already carry its id
   * @return  Item registry object
   */
  public <I extends Item> ItemObject<I> register(String name, Supplier<Item.Properties> props, Function<Item.Properties, ? extends I> factory) {
    ResourceKey<Item> key = key(Registries.ITEM, name);
    return new ItemObject<>(register.register(name, () -> factory.apply(props.get().setId(key))));
  }

  /**
   * Adds a new item to the list to be registered, starting from default properties.
   * @param name     Item name
   * @param factory  Creates the item from properties that already carry its id, for example {@code p -> new Item(p.stacksTo(16))}
   * @return  Item registry object
   */
  public <I extends Item> ItemObject<I> register(String name, Function<Item.Properties, ? extends I> factory) {
    return register(name, Item.Properties::new, factory);
  }

  /**
   * Adds a new plain item with default properties.
   * @param name   Item name
   * @return  Item registry object
   */
  public ItemObject<Item> register(String name) {
    return register(name, Item::new);
  }


  /* Specialty */

  /**
   * Registers an item with multiple variants, prefixing the name with the value name
   * @param values   Enum values to use for this item
   * @param name     Name of the item
   * @param mapper   Creates the item for an enum value from properties that already carry its id
   * @return  EnumObject mapping between different item types
   */
  public <T extends Enum<T>, I extends Item> EnumObject<T,I> registerEnum(T[] values, String name, BiFunction<T, Item.Properties, ? extends I> mapper) {
    return buildEnumObject(values, name, (fullName, type) -> register(fullName, props -> mapper.apply(type, props)));
  }

  /**
   * Registers an item with multiple variants, suffixing the name with the value name
   * @param name     Name of the item
   * @param values   Enum values to use for this item
   * @param mapper   Creates the item for an enum value from properties that already carry its id
   * @return  EnumObject mapping between different item types
   */
  public <T extends Enum<T>, I extends Item> EnumObject<T,I> registerEnum(String name, T[] values, BiFunction<T, Item.Properties, ? extends I> mapper) {
    return buildEnumObject(name, values, (fullName, type) -> register(fullName, props -> mapper.apply(type, props)));
  }
}
