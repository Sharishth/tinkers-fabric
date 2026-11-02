package slimeknights.mantle.registration.deferred;

import net.fabricmc.fabric.api.item.v1.BlockTransformerHelper;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import slimeknights.mantle.block.MantleButtonBlock;
import slimeknights.mantle.block.MantleDoorBlock;
import slimeknights.mantle.block.MantlePressurePlateBlock;
import slimeknights.mantle.block.MantleStairBlock;
import slimeknights.mantle.block.MantleTrapDoorBlock;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.EnumObject.Builder;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;

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
  /** Ticks a wooden button stays pressed, as vanilla wooden buttons */
  private static final int WOOD_BUTTON_PRESSED_TICKS = 30;

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



  /* Building */

  /**
   * Registers a plain block with slab and stairs, all made from the same base properties
   * @param name   Block name
   * @param props  Creates the base properties, called once per block; the id is set afterwards
   * @param item   Creates the block item, used for all variants
   * @return  Building block object
   */
  public BuildingBlockObject registerBuilding(String name, Supplier<BlockBehaviour.Properties> props, BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    ItemObject<Block> blockObj = register(name, props, Block::new, item);
    return new BuildingBlockObject(blockObj,
      register(name + "_slab", props, SlabBlock::new, item),
      register(name + "_stairs", props, p -> new MantleStairBlock(blockObj.get().defaultBlockState(), p), item));
  }

  /**
   * Registers a custom block with slab and stairs copying its properties
   * @param name     Block name
   * @param props    Creates the base block properties; the id is set afterwards
   * @param factory  Creates the base block from properties that already carry its id
   * @param item     Creates the block item, used for all variants
   * @return  Building block object
   */
  public BuildingBlockObject registerBuilding(String name, Supplier<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends Block> factory,
                                              BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    ItemObject<Block> blockObj = register(name, props, factory, item);
    // copies are taken at registration, when the base block exists (it is an earlier entry)
    Supplier<BlockBehaviour.Properties> copy = () -> BlockBehaviour.Properties.ofFullCopy(blockObj.get());
    return new BuildingBlockObject(blockObj,
      register(name + "_slab", copy, SlabBlock::new, item),
      register(name + "_stairs", copy, p -> new MantleStairBlock(blockObj.get().defaultBlockState(), p), item));
  }

  /**
   * Registers a plain block with slab, stairs, and wall
   * @param name   Name of the block
   * @param props  Creates the base properties, called once per block; the id is set afterwards
   * @param item   Creates the block item, used for all variants
   * @return  Wall building block object
   */
  public WallBuildingBlockObject registerWallBuilding(String name, Supplier<BlockBehaviour.Properties> props, BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    return new WallBuildingBlockObject(registerBuilding(name, props, item), register(name + "_wall", props, WallBlock::new, item));
  }

  /**
   * Registers a custom block with slab, stairs, and wall copying its properties
   * @param name     Block name
   * @param props    Creates the base block properties; the id is set afterwards
   * @param factory  Creates the base block from properties that already carry its id
   * @param item     Creates the block item, used for all variants
   * @return  Wall building block object
   */
  public WallBuildingBlockObject registerWallBuilding(String name, Supplier<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends Block> factory,
                                                      BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    BuildingBlockObject obj = registerBuilding(name, props, factory, item);
    return new WallBuildingBlockObject(obj, register(name + "_wall", () -> BlockBehaviour.Properties.ofFullCopy(obj.get()), WallBlock::new, item));
  }

  /**
   * Registers a plain block with slab, stairs, and fence
   * @param name   Name of the block
   * @param props  Creates the base properties, called once per block; the id is set afterwards
   * @param item   Creates the block item, used for all variants
   * @return  Fence building block object
   */
  public FenceBuildingBlockObject registerFenceBuilding(String name, Supplier<BlockBehaviour.Properties> props, BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    return new FenceBuildingBlockObject(registerBuilding(name, props, item), register(name + "_fence", props, FenceBlock::new, item));
  }

  /**
   * Registers a custom block with slab, stairs, and fence copying its properties
   * @param name     Block name
   * @param props    Creates the base block properties; the id is set afterwards
   * @param factory  Creates the base block from properties that already carry its id
   * @param item     Creates the block item, used for all variants
   * @return  Fence building block object
   */
  public FenceBuildingBlockObject registerFenceBuilding(String name, Supplier<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends Block> factory,
                                                        BiFunction<? super Block, Item.Properties, ? extends BlockItem> item) {
    BuildingBlockObject obj = registerBuilding(name, props, factory, item);
    return new FenceBuildingBlockObject(obj, register(name + "_fence", () -> BlockBehaviour.Properties.ofFullCopy(obj.get()), FenceBlock::new, item));
  }


  /* Metal */

  /**
   * Creates a new metal item object: storage block, ingot and nugget
   * @param name        Metal name
   * @param tagName     Name to use for the common tags of this metal
   * @param blockProps  Creates the block properties; the id is set afterwards
   * @param blockItem   Creates the block item
   * @param itemProps   Creates the base properties for the ingot and nugget, called once per item
   * @return  Metal item object
   */
  public MetalItemObject registerMetal(String name, String tagName, Supplier<BlockBehaviour.Properties> blockProps,
                                       BiFunction<? super Block, Item.Properties, ? extends BlockItem> blockItem, Supplier<Item.Properties> itemProps) {
    ItemObject<Block> block = register(name + "_block", blockProps, blockItem);
    RegistryObject<Item> ingot = registerPlainItem(name + "_ingot", itemProps);
    RegistryObject<Item> nugget = registerPlainItem(name + "_nugget", itemProps);
    return new MetalItemObject(tagName, block, ingot, nugget);
  }

  /**
   * Creates a new metal item object, using the metal name for its tags
   * @param name        Metal name
   * @param blockProps  Creates the block properties; the id is set afterwards
   * @param blockItem   Creates the block item
   * @param itemProps   Creates the base properties for the ingot and nugget, called once per item
   * @return  Metal item object
   */
  public MetalItemObject registerMetal(String name, Supplier<BlockBehaviour.Properties> blockProps,
                                       BiFunction<? super Block, Item.Properties, ? extends BlockItem> blockItem, Supplier<Item.Properties> itemProps) {
    return registerMetal(name, name, blockProps, blockItem, itemProps);
  }

  /** Registers a plain item in this register's item register, with the id set on its properties */
  private RegistryObject<Item> registerPlainItem(String name, Supplier<Item.Properties> props) {
    ResourceKey<Item> key = key(Registries.ITEM, name);
    return itemRegister.register(name, () -> new Item(props.get().setId(key)));
  }


  /* Wood */

  /**
   * Registers a full wood set: planks with slab, stairs and fence, logs and wood with stripped forms,
   * fence gate, door, trapdoor, pressure plate, button, and standing, wall and hanging signs.
   * Stripping and the sign block entity types are wired after {@link RegistrationQueue#flush()}, so call {@link #register()}.
   * @param name             Name of the wood type, the prefix of every block
   * @param behaviorCreator  Creates fresh base properties for a variant, called once per block
   * @param flammable        If true, the items burn as furnace fuel like vanilla wood
   * @return  Wood object
   */
  public WoodBlockObject registerWood(String name, Function<WoodVariant, BlockBehaviour.Properties> behaviorCreator, boolean flammable) {
    // WHY Fabric builders: 26.3 has no public WoodType/BlockSetType register; the builders also fix the sign edit screens
    BlockSetType setType = new BlockSetTypeBuilder().register(resource(name));
    WoodType woodType = new WoodTypeBuilder().register(resource(name), setType);
    WoodSettings wood = new WoodSettings(behaviorCreator, flammable);
    BiFunction<Block, Item.Properties, BlockItem> woodBlockItem = wood.item(ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);

    // planks
    BuildingBlockObject planks = registerBuilding(name + "_planks", () -> wood.props(WoodVariant.PLANKS).strength(2.0f, 3.0f),
      (block, props) -> new BlockItem(block, wood.fuel(props, block instanceof SlabBlock ? ContextIntProviders.COOKING_TIME_WOOD_SLABS : ContextIntProviders.COOKING_TIME_WOOD_BLOCKS)));
    ItemObject<FenceBlock> fence = register(name + "_fence", () -> BlockBehaviour.Properties.ofFullCopy(planks.get()).forceSolidOn(), FenceBlock::new, woodBlockItem);
    // logs and wood
    Supplier<BlockBehaviour.Properties> strippedProps = () -> wood.props(WoodVariant.PLANKS).strength(2.0f);
    ItemObject<RotatedPillarBlock> strippedLog = register("stripped_" + name + "_log", strippedProps, RotatedPillarBlock::new, woodBlockItem);
    ItemObject<RotatedPillarBlock> strippedWood = register("stripped_" + name + "_wood", strippedProps, RotatedPillarBlock::new, woodBlockItem);
    ItemObject<RotatedPillarBlock> log = register(name + "_log", () -> wood.props(WoodVariant.LOG).strength(2.0f), RotatedPillarBlock::new, woodBlockItem);
    ItemObject<RotatedPillarBlock> logWood = register(name + "_wood", () -> wood.props(WoodVariant.WOOD).strength(2.0f), RotatedPillarBlock::new, woodBlockItem);
    // WHY after flush: replaces upstream StrippableLogBlock's Forge tool hook; the helper needs the constructed blocks
    register.afterFlush(() -> {
      BlockTransformerHelper.registerStripping(log.get(), strippedLog.get());
      BlockTransformerHelper.registerStripping(logWood.get(), strippedWood.get());
    });

    // doors
    ItemObject<MantleDoorBlock> door = register(name + "_door", () -> wood.props(WoodVariant.PLANKS).strength(3.0f).noOcclusion().pushReaction(PushReaction.POPPED),
      p -> new MantleDoorBlock(setType, p), (block, props) -> new DoubleHighBlockItem(block, wood.fuel(props, ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE)));
    ItemObject<MantleTrapDoorBlock> trapdoor = register(name + "_trapdoor", () -> wood.props(WoodVariant.PLANKS).strength(3.0f).noOcclusion().isValidSpawn(BlockDeferredRegister::neverSpawn),
      p -> new MantleTrapDoorBlock(setType, p), woodBlockItem);
    ItemObject<FenceGateBlock> fenceGate = register(name + "_fence_gate", () -> BlockBehaviour.Properties.ofFullCopy(fence.get()), p -> new FenceGateBlock(woodType, p), woodBlockItem);
    // redstone
    Supplier<BlockBehaviour.Properties> redstoneProps = () -> wood.props(WoodVariant.PLANKS).forceSolidOn().noCollision().pushReaction(PushReaction.POPPED).strength(0.5f);
    ItemObject<MantlePressurePlateBlock> pressurePlate = register(name + "_pressure_plate", redstoneProps, p -> new MantlePressurePlateBlock(setType, p), woodBlockItem);
    ItemObject<MantleButtonBlock> button = register(name + "_button", redstoneProps, p -> new MantleButtonBlock(setType, WOOD_BUTTON_PRESSED_TICKS, p),
      wood.item(ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL));

    // signs
    Supplier<BlockBehaviour.Properties> signProps = () -> wood.props(WoodVariant.PLANKS).forceSolidOn().noCollision().strength(1.0f);
    RegistryObject<StandingSignBlock> sign = registerNoItem(name + "_sign", signProps, p -> new StandingSignBlock(woodType, p));
    RegistryObject<WallSignBlock> wallSign = registerNoItem(name + "_wall_sign", () -> wallVariant(signProps.get(), sign.get()), p -> new WallSignBlock(woodType, p));
    RegistryObject<CeilingHangingSignBlock> hangingSign = registerNoItem(name + "_hanging_sign", signProps, p -> new CeilingHangingSignBlock(woodType, p));
    RegistryObject<WallHangingSignBlock> wallHangingSign = registerNoItem(name + "_wall_hanging_sign", () -> wallVariant(signProps.get(), hangingSign.get()),
      p -> new WallHangingSignBlock(woodType, p));
    registerSignItems(name, wood, sign, wallSign, hangingSign, wallHangingSign);

    return new WoodBlockObject(resource(name), woodType, planks, log, strippedLog, logWood, strippedWood,
                               fence, fenceGate, door, trapdoor, pressurePlate, button,
                               sign, wallSign, hangingSign, wallHangingSign);
  }

  /**
   * Registers the sign and hanging sign items, and adds all four sign blocks to the vanilla sign block entity types.
   * WHY vanilla block entity types: Fabric's addValidBlock opens their block set, so upstream's MantleSignBlockEntity copies are not needed.
   */
  private void registerSignItems(String name, WoodSettings wood, Supplier<StandingSignBlock> sign, Supplier<WallSignBlock> wallSign,
                                 Supplier<CeilingHangingSignBlock> hangingSign, Supplier<WallHangingSignBlock> wallHangingSign) {
    ResourceKey<Item> signKey = key(Registries.ITEM, name + "_sign");
    itemRegister.register(name + "_sign", () -> new StandingAndWallBlockItem(sign.get(), wallSign.get(), Direction.DOWN,
      wood.fuel(signItemProperties(signKey), ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE)));
    ResourceKey<Item> hangingSignKey = key(Registries.ITEM, name + "_hanging_sign");
    itemRegister.register(name + "_hanging_sign", () -> new HangingSignItem(hangingSign.get(), wallHangingSign.get(),
      wood.fuel(signItemProperties(hangingSignKey), ContextIntProviders.COOKING_TIME_HANGING_SIGNS)));
    register.afterFlush(() -> {
      BlockEntityTypes.SIGN.addValidBlock(sign.get());
      BlockEntityTypes.SIGN.addValidBlock(wallSign.get());
      BlockEntityTypes.HANGING_SIGN.addValidBlock(hangingSign.get());
      BlockEntityTypes.HANGING_SIGN.addValidBlock(wallHangingSign.get());
    });
  }

  /** Item properties for a sign item, as vanilla sign items: stacks of 16, editable text */
  private static Item.Properties signItemProperties(ResourceKey<Item> key) {
    return new Item.Properties().useBlockDescriptionPrefix().setId(key).stacksTo(16).signText();
  }

  /**
   * Makes a wall sign drop and display as its standing sign, like vanilla's private Blocks.wallVariant.
   * Replaces upstream's lootFrom; the standing sign is an earlier entry, so it exists when this runs.
   */
  private static BlockBehaviour.Properties wallVariant(BlockBehaviour.Properties props, Block standing) {
    return props.overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
  }

  /** Spawn predicate that never allows spawning; vanilla's Blocks.never is private in 26.3 */
  private static boolean neverSpawn(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
    return false;
  }

  /**
   * Shared settings for the blocks and items of one wood type.
   * @param behaviorCreator  Creates fresh base properties for a variant
   * @param flammable        If true, items get vanilla wood burn times
   */
  private record WoodSettings(Function<WoodVariant, BlockBehaviour.Properties> behaviorCreator, boolean flammable) {
    /** Fresh properties for a variant, with the wooden note block sound */
    BlockBehaviour.Properties props(WoodVariant variant) {
      return behaviorCreator.apply(variant).instrument(NoteBlockInstrument.BASS);
    }

    /**
     * Adds a burn time when the wood is flammable.
     * WHY keys: 26.3 burn times are data-driven values, replacing upstream's Burnable*Item classes and their tick counts.
     */
    Item.Properties fuel(Item.Properties props, ResourceKey<ContextIntProvider> burnTime) {
      return flammable ? props.cookingFuel(burnTime) : props;
    }

    /** Block item factory with the given burn time */
    BiFunction<Block, Item.Properties, BlockItem> item(ResourceKey<ContextIntProvider> burnTime) {
      return (block, props) -> new BlockItem(block, fuel(props, burnTime));
    }
  }
}
