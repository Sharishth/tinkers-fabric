package slimeknights.mantle.gametest.registration;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import slimeknights.mantle.registration.deferred.BlockDeferredRegister;
import slimeknights.mantle.registration.deferred.ItemDeferredRegister;
import slimeknights.mantle.registration.deferred.RegistrationQueue;
import slimeknights.mantle.registration.deferred.RegistryObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;

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

  @Override
  public void onInitialize() {
    // WHY items queued before blocks: proves the queue reorders registries the way Forge events did
    ITEMS.register();
    BLOCKS.register();
    RegistrationQueue.flush();
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
