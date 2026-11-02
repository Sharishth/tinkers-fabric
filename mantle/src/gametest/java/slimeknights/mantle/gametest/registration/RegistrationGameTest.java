package slimeknights.mantle.gametest.registration;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.gametest.registration.RegistrationTestContent.Variant;

import static slimeknights.mantle.gametest.registration.RegistrationTestContent.BLOCK_AWARE_ITEM;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.NAMESPACE;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.NO_ITEM_BLOCK;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.PLAIN_BLOCK;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.PLAIN_ITEM;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.POTTED_PLAIN_BLOCK;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.STACK_16_ITEM;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.VARIANT_BLOCKS;

/** Checks Mantle's deferred registers against the real game registries. */
public class RegistrationGameTest {
  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(NAMESPACE, path);
  }

  @GameTest
  public void entriesAreInRegistries(GameTestHelper helper) {
    for (String block : new String[] {"plain_block", "no_item_block", "alpha_variant_block", "beta_variant_block", "potted_plain_block"}) {
      helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(block)), "missing block " + block);
    }
    for (String item : new String[] {"plain_item", "stack_16_item", "block_aware_item", "plain_block", "alpha_variant_block", "beta_variant_block"}) {
      helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(item)), "missing item " + item);
    }
    helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id("no_item_block")), "no_item_block must not have an item");
    helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id("potted_plain_block")), "pots must not have an item");
    helper.succeed();
  }

  @GameTest
  public void handlesResolveToRegisteredInstances(GameTestHelper helper) {
    helper.assertTrue(PLAIN_BLOCK.get() == BuiltInRegistries.BLOCK.getValue(id("plain_block")), "block handle is the registered block");
    helper.assertTrue(PLAIN_ITEM.get() == BuiltInRegistries.ITEM.getValue(id("plain_item")), "item handle is the registered item");
    helper.assertTrue(NO_ITEM_BLOCK.getHolder().map(holder -> holder.value() == NO_ITEM_BLOCK.get()).orElse(false), "holder bound to the value");
    helper.assertTrue(VARIANT_BLOCKS.get(Variant.BETA) == BuiltInRegistries.BLOCK.getValue(id("beta_variant_block")), "enum entry uses value prefix");
    helper.succeed();
  }

  @GameTest
  public void blockItemsAreWiredLikeVanilla(GameTestHelper helper) {
    Block block = PLAIN_BLOCK.get();
    Item item = block.asItem();
    helper.assertTrue(item instanceof BlockItem blockItem && blockItem.getBlock() == block, "Block.asItem returns its BlockItem (Item.BY_BLOCK)");
    helper.assertTrue(item == BuiltInRegistries.ITEM.getValue(id("plain_block")), "block item has the block's id");
    helper.assertTrue("block.mantle_test.plain_block".equals(block.getDescriptionId()), "block id set from registry key: " + block.getDescriptionId());
    helper.assertTrue(block.getDescriptionId().equals(item.getDescriptionId()), "block item uses the block translation key: " + item.getDescriptionId());
    helper.succeed();
  }

  @GameTest
  public void itemPropertiesCarryIdAndCustomizations(GameTestHelper helper) {
    helper.assertTrue("item.mantle_test.plain_item".equals(PLAIN_ITEM.get().getDescriptionId()), "item id set: " + PLAIN_ITEM.get().getDescriptionId());
    helper.assertTrue(STACK_16_ITEM.get().getDefaultMaxStackSize() == 16, "custom stack size kept");
    helper.assertTrue(PLAIN_ITEM.get().getDefaultMaxStackSize() == 64, "default stack size");
    helper.succeed();
  }

  @GameTest
  public void blocksRegisterBeforeItems(GameTestHelper helper) {
    helper.assertTrue(BLOCK_AWARE_ITEM.get().block == PLAIN_BLOCK.get(), "item constructor saw the registered block");
    helper.succeed();
  }

  @GameTest
  public void pottedBlockHoldsItsPlant(GameTestHelper helper) {
    helper.assertTrue(POTTED_PLAIN_BLOCK.get().getPotted() == PLAIN_BLOCK.get(), "pot contains the plain block");
    helper.succeed();
  }
}
