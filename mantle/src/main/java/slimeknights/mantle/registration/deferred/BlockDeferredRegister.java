package slimeknights.mantle.registration.deferred;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.EnumObject.Builder;
import slimeknights.mantle.registration.object.ItemObject;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Deferred register to handle registering blocks with possible item forms.
 * WHY factories: 26.x blocks and items need their id in their properties before construction,
 * so the register creates the properties with the id set and passes them on (D-18).
 * Block items also get {@link Item.Properties#useBlockDescriptionPrefix()}, as vanilla block items do.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BlockDeferredRegister extends DeferredRegisterWrapper<Block> {
  protected final SynchronizedDeferredRegister<Item> itemRegister;

  public BlockDeferredRegister(String modID) {
    super(Registries.BLOCK, modID);
    this.itemRegister = SynchronizedDeferredRegister.create(Registries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    itemRegister.register();
  }

  /**
   * Properties for flower pots; a new instance each call because setId mutates them.
   * 26.3 renamed PushReaction.DESTROY to POPPED (vanilla flower pots use POPPED).
   */
  protected static BlockBehaviour.Properties pottedProperties() {
    return BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.POPPED);
  }


  /* Blocks with no items */

  /**
   * Registers a block with the block registry
   * @param name     Block ID
   * @param props    Creates the base properties, called once; the id is set afterwards
   * @param factory  Creates the block from properties that already carry its id
   * @param <B>      Block class
   * @return  Block registry object
   */
  public <B extends Block> RegistryObject<B> registerNoItem(String name, Supplier<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends B> factory) {
    ResourceKey<Block> key = key(Registries.BLOCK, name);
    return register.register(name, () -> factory.apply(props.get().setId(key)));
  }

  /**
   * Registers a plain block with the block registry
   * @param name   Block ID
   * @param props  Creates the base properties, called once; the id is set afterwards
   * @return  Block registry object
   */
  public RegistryObject<Block> registerNoItem(String name, Supplier<BlockBehaviour.Properties> props) {
    return registerNoItem(name, props, Block::new);
  }


  /* Block item pairs */

  /**
   * Registers a block with the block registry, using the function for the BlockItem
   * @param name     Block ID
   * @param props    Creates the base block properties; the id is set afterwards
   * @param factory  Creates the block from properties that already carry its id
   * @param item     Creates the block item from the block and item properties that already carry the item id
   * @param <B>      Block class
   * @return  Block item registry object pair
   */
  public <B extends Block> ItemObject<B> register(String name, Supplier<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends B> factory,
                                                  BiFunction<? super B, Item.Properties, ? extends BlockItem> item) {
    RegistryObject<B> blockObj = registerNoItem(name, props, factory);
    ResourceKey<Item> itemKey = key(Registries.ITEM, name);
    itemRegister.register(name, () -> item.apply(blockObj.get(), new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
    return new ItemObject<>(blockObj);
  }

  /**
   * Registers a plain block with the block registry, using the function for the BlockItem
   * @param name   Block ID
   * @param props  Creates the base block properties; the id is set afterwards
   * @param item   Creates the block item, for example {@code BlockItem::new}
   * @return  Block item registry object pair
   */
  public ItemObject<Block> register(String name, Supplier<BlockBehaviour.Properties> props, BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    return register(name, props, Block::new, item);
  }


  /* Potted */

  /**
   * Registers a potted form of the given block using the vanilla pot.
   * The vanilla pot learns the new plant when the potted block is constructed, so no extra mapping is needed.
   * @param name  Name of the flower
   * @param block Block to put in the pot; registered before the pot since it is in an earlier entry
   * @return  Potted block instance
   */
  public RegistryObject<FlowerPotBlock> registerPotted(String name, Supplier<? extends Block> block) {
    return registerNoItem("potted_" + name, BlockDeferredRegister::pottedProperties, props -> new FlowerPotBlock(block.get(), props));
  }

  /** Registers a potted form of the given block using the vanilla pot */
  public RegistryObject<FlowerPotBlock> registerPotted(RegistryObject<? extends Block> block) {
    return registerPotted(block.getId().getPath(), block);
  }

  /** Registers a potted form of the given block using the vanilla pot */
  public RegistryObject<FlowerPotBlock> registerPotted(ItemObject<? extends Block> block) {
    return registerPotted(block.getId().getPath(), block);
  }


  /* Enum */

  /**
   * Registers a block with multiple variants, prefixing the name with the value name
   * @param values   Enum values to use for this block
   * @param name     Name of the block
   * @param props    Creates the base block properties for a value; the id is set afterwards
   * @param mapper   Creates the block for a value from properties that already carry its id
   * @param item     Creates the block item from the block and item properties that already carry the item id
   * @return  EnumObject mapping between different block types
   */
  public <T extends Enum<T>, B extends Block> EnumObject<T,B> registerEnum(T[] values, String name, Function<T, BlockBehaviour.Properties> props,
                                                                          BiFunction<T, BlockBehaviour.Properties, ? extends B> mapper,
                                                                          BiFunction<? super B, Item.Properties, ? extends BlockItem> item) {
    return buildEnumObject(values, name, (fullName, value) -> register(fullName, () -> props.apply(value), p -> mapper.apply(value, p), item));
  }

  /**
   * Registers a block with multiple variants, suffixing the name with the value name
   * @param name     Name of the block
   * @param values   Enum values to use for this block
   * @param props    Creates the base block properties for a value; the id is set afterwards
   * @param mapper   Creates the block for a value from properties that already carry its id
   * @param item     Creates the block item from the block and item properties that already carry the item id
   * @return  EnumObject mapping between different block types
   */
  public <T extends Enum<T>, B extends Block> EnumObject<T,B> registerEnum(String name, T[] values, Function<T, BlockBehaviour.Properties> props,
                                                                          BiFunction<T, BlockBehaviour.Properties, ? extends B> mapper,
                                                                          BiFunction<? super B, Item.Properties, ? extends BlockItem> item) {
    return buildEnumObject(name, values, (fullName, value) -> register(fullName, () -> props.apply(value), p -> mapper.apply(value, p), item));
  }

  /**
   * Registers a block with enum variants, but no item form
   * @param values  Enum value list
   * @param name    Suffix after value name
   * @param props   Creates the base block properties for a value; the id is set afterwards
   * @param mapper  Creates the block for a value from properties that already carry its id
   * @return  Enum object
   */
  public <T extends Enum<T>, B extends Block> EnumObject<T, B> registerEnumNoItem(T[] values, String name, Function<T, BlockBehaviour.Properties> props,
                                                                                 BiFunction<T, BlockBehaviour.Properties, ? extends B> mapper) {
    return buildEnumObject(values, name, (fullName, value) -> registerNoItem(fullName, () -> props.apply(value), p -> mapper.apply(value, p)));
  }

  /** Registers a potted form of the given block using the vanilla pot */
  public <T extends Enum<T> & StringRepresentable, B extends Block> EnumObject<T, FlowerPotBlock> registerPottedEnum(T[] values, String name, EnumObject<T, B> block) {
    EnumObject.Builder<T, FlowerPotBlock> builder = new Builder<>(values[0].getDeclaringClass());
    for (T value : values) {
      Supplier<? extends B> supplier = block.getSupplier(value);
      if (supplier != null) {
        builder.put(value, registerPotted(value.getSerializedName() + "_" + name, supplier));
      }
    }
    return builder.build();
  }

  /** Registers a potted form of the given blocks using the vanilla pot, automatically choosing the values based on the passed object */
  public <T extends Enum<T> & StringRepresentable, B extends Block> EnumObject<T, FlowerPotBlock> registerPottedEnum(String name, EnumObject<T, B> block) {
    Collection<Entry<T,Supplier<? extends B>>> entries = block.entries();
    EnumObject.Builder<T, FlowerPotBlock> builder = new Builder<>(entries.iterator().next().getKey().getDeclaringClass());
    for (Entry<T,Supplier<? extends B>> entry : entries) {
      T value = entry.getKey();
      builder.put(value, registerPotted(value.getSerializedName() + "_" + name, entry.getValue()));
    }
    return builder.build();
  }

  // TODO(T-12b): building blocks (slab/stairs/wall/fence) and metal objects
  // TODO(T-12c): wood objects with signs, doors, trapdoors, pressure plates and buttons
}
