package slimeknights.mantle.gametest.data;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.util.ItemStackList;

/** Checks util helpers that need item components or a level */
public class UtilGameTest {
  @GameTest
  public void itemStackListCopiesAndReadsContainers(GameTestHelper helper) {
    SimpleContainer container = new SimpleContainer(new ItemStack(Items.STICK), ItemStack.EMPTY, new ItemStack(Items.APPLE, 3));
    // upstream threw here: it called add on a fixed-size list
    ItemStackList list = ItemStackList.of(container);
    helper.assertValueEqual(list.size(), 3, "one entry per slot");
    helper.assertTrue(list.hasItem(0) && !list.hasItem(1) && list.hasItem(2), "slot contents");
    // upstream threw here: it called set on an empty growable copy
    ItemStackList growable = list.copy(false);
    growable.add(new ItemStack(Items.DIRT));
    helper.assertValueEqual(growable.size(), 4, "growable copy");
    ItemStackList deep = list.deepCopy(true);
    deep.get(2).shrink(1);
    helper.assertValueEqual(list.get(2).getCount(), 3, "deep copy does not share stacks");
    list.setEmpty(0);
    helper.assertTrue(!list.hasItem(0), "slot emptied");
    helper.succeed();
  }

  @GameTest
  public void blockEntityHelperChecksLoadedChunks(GameTestHelper helper) {
    BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
    helper.assertTrue(BlockEntityHelper.isBlockLoaded(helper.getLevel(), pos), "test area is loaded");
    helper.assertTrue(!BlockEntityHelper.isBlockLoaded(null, pos), "no level means not loaded");
    helper.succeed();
  }
}
