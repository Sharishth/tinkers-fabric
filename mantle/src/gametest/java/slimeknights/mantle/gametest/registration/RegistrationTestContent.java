package slimeknights.mantle.gametest.registration;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.registration.deferred.AttributeDeferredRegister;
import slimeknights.mantle.registration.deferred.BlockDeferredRegister;
import slimeknights.mantle.registration.deferred.BlockEntityTypeDeferredRegister;
import slimeknights.mantle.registration.deferred.EntityTypeDeferredRegister;
import slimeknights.mantle.registration.deferred.EnumDeferredRegister;
import slimeknights.mantle.registration.deferred.ItemDeferredRegister;
import slimeknights.mantle.registration.deferred.MenuTypeDeferredRegister;
import slimeknights.mantle.registration.deferred.PotionDeferredRegister;
import slimeknights.mantle.registration.deferred.PotionDeferredRegister.PotionType;
import slimeknights.mantle.registration.deferred.RegistrationQueue;
import slimeknights.mantle.registration.deferred.RegistryObject;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.EntityObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;

import java.util.Locale;

/**
 * Test-only content registered through Mantle's deferred registers.
 * Lives in the gametest source set, so it never ships and never appears in a normal dev client.
 */
public class RegistrationTestContent implements ModInitializer {
  public static final String NAMESPACE = "mantle_test";

  public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(NAMESPACE);
  public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(NAMESPACE);

  public static final ItemObject<Item> PLAIN_ITEM = ITEMS.register("plain_item");
  public static final ItemObject<Item> STACK_16_ITEM = ITEMS.register("stack_16_item", props -> new Item(props.stacksTo(16)));
  public static final ItemObject<Block> PLAIN_BLOCK = BLOCKS.register("plain_block", BlockBehaviour.Properties::of, BlockItem::new);
  /** Reads a block while it is constructed; only works if blocks register before items */
  public static final ItemObject<BlockAwareItem> BLOCK_AWARE_ITEM = ITEMS.register("block_aware_item", props -> new BlockAwareItem(PLAIN_BLOCK.get(), props));
  public static final RegistryObject<Block> NO_ITEM_BLOCK = BLOCKS.registerNoItem("no_item_block", BlockBehaviour.Properties::of);
  public static final EnumObject<Variant, Block> VARIANT_BLOCKS = BLOCKS.registerEnum(
    Variant.values(), "variant_block", variant -> BlockBehaviour.Properties.of(), (variant, props) -> new Block(props), BlockItem::new);
  public static final RegistryObject<FlowerPotBlock> POTTED_PLAIN_BLOCK = BLOCKS.registerPotted(PLAIN_BLOCK);

  /* T-12b content */
  public static final EntityTypeDeferredRegister ENTITIES = new EntityTypeDeferredRegister(NAMESPACE);
  public static final BlockEntityTypeDeferredRegister BLOCK_ENTITIES = new BlockEntityTypeDeferredRegister(NAMESPACE);
  public static final MenuTypeDeferredRegister MENUS = new MenuTypeDeferredRegister(NAMESPACE);
  public static final AttributeDeferredRegister ATTRIBUTES = new AttributeDeferredRegister(NAMESPACE);
  public static final PotionDeferredRegister POTIONS = new PotionDeferredRegister(NAMESPACE);
  public static final EnumDeferredRegister<SoundEvent> SOUNDS = new EnumDeferredRegister<>(Registries.SOUND_EVENT, NAMESPACE);

  public static final BuildingBlockObject BUILDING = BLOCKS.registerBuilding("test_brick", BlockBehaviour.Properties::of, BlockItem::new);
  /** Custom base block: slab, stairs and wall copy its properties */
  public static final WallBuildingBlockObject WALL_BUILDING = BLOCKS.registerWallBuilding("test_stone", () -> BlockBehaviour.Properties.of().strength(3f), Block::new, BlockItem::new);
  public static final FenceBuildingBlockObject FENCE_BUILDING = BLOCKS.registerFenceBuilding("test_plank", BlockBehaviour.Properties::of, BlockItem::new);
  public static final MetalItemObject METAL = BLOCKS.registerMetal("test_metal", BlockBehaviour.Properties::of, BlockItem::new, Item.Properties::new);
  public static final EntityObject<Zombie> TEST_ZOMBIE = ENTITIES.registerWithEgg("test_zombie", () -> EntityType.Builder.of(Zombie::new, MobCategory.MONSTER));
  public static final RegistryObject<BlockEntityType<TestBlockEntity>> TEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("test_block_entity", TestBlockEntity::new, PLAIN_BLOCK);
  public static final RegistryObject<MenuType<ChestMenu>> PLAIN_MENU = MENUS.register("test_menu", ChestMenu::threeRows);
  public static final RegistryObject<ExtendedMenuType<ChestMenu, BlockPos>> EXTENDED_MENU = MENUS.register(
    "test_extended_menu", (int id, Inventory inventory, BlockPos pos) -> ChestMenu.threeRows(id, inventory), BlockPos.STREAM_CODEC);
  public static final RegistryObject<Attribute> TEST_ATTRIBUTE = ATTRIBUTES.register("test_attribute", 1, 0, 10, true);
  /** 3 minutes; strong = level 2 for 1.5 minutes, long = 8 minutes */
  public static final EnumObject<PotionType, Potion> TEST_POTIONS = POTIONS.registerTypes("test_potion", () -> MobEffects.SPEED, 3600, 0).withStrong().withLong().build();
  public static final EnumObject<Variant, SoundEvent> TEST_SOUNDS = SOUNDS.registerEnum(
    "test_sound", Variant.values(), variant -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(NAMESPACE, "test_sound_" + variant.getSerializedName())));

  @Override
  public void onInitialize() {
    // WHY items queued before blocks: proves the queue reorders registries the way Forge events did
    ITEMS.register();
    BLOCKS.register();
    ENTITIES.register();
    BLOCK_ENTITIES.register();
    MENUS.register();
    ATTRIBUTES.register();
    POTIONS.register();
    SOUNDS.register();
    RegistrationQueue.flush();
    // a mob type without default attributes makes vanilla log errors at startup
    FabricDefaultAttributeRegistry.register(TEST_ZOMBIE.get(), Zombie.createAttributes());
  }

  /** Block entity used for the block entity type test */
  public static class TestBlockEntity extends BlockEntity {
    public TestBlockEntity(BlockPos pos, BlockState state) {
      super(TEST_BLOCK_ENTITY.get(), pos, state);
    }
  }

  /** Item that captures a block during construction */
  public static class BlockAwareItem extends Item {
    public final Block block;

    public BlockAwareItem(Block block, Properties properties) {
      super(properties);
      this.block = block;
    }
  }

  /** Enum used for the enum registration test */
  public enum Variant implements StringRepresentable {
    ALPHA, BETA;

    @Override
    public String getSerializedName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
