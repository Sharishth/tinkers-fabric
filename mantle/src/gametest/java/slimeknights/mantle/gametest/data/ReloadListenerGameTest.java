package slimeknights.mantle.gametest.data;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import slimeknights.mantle.gametest.data.ReloadTestContent.EnchantedInput;
import slimeknights.mantle.gametest.data.ReloadTestContent.MergedEntry;
import slimeknights.mantle.gametest.registration.RegistrationTestContent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Checks reload listeners parse with world registries.
 * The data of {@link ReloadTestContent} holds enchantments and a tag ingredient, which only load when the listener
 * put {@code ContextKey.REGISTRIES} into the parse context (D-21).
 */
public class ReloadListenerGameTest {
  /** Ticks allowed for a full data reload; the default of 20 is too short for one */
  private static final int RELOAD_TICKS = 1200;
  private static final Identifier MERGED_SAMPLE = Identifier.fromNamespaceAndPath(RegistrationTestContent.NAMESPACE, "sample");

  @GameTest
  public void registryDataMapParsesWithWorldRegistries(GameTestHelper helper) {
    EnchantedInput diamond = ReloadTestContent.ITEM_DATA.get(Items.DIAMOND_SWORD);
    helper.assertTrue(diamond != null, "diamond sword data loaded; without registries in the context the enchantment fails to parse");
    helper.assertTrue(diamond.enchantment().is(Enchantments.SHARPNESS), "datapack registry entry resolved, got " + diamond.enchantment());
    helper.assertTrue(diamond.input().test(new ItemStack(Items.OAK_LOG)), "tag ingredient matches a tagged item");
    helper.assertFalse(diamond.input().test(new ItemStack(Items.STICK)), "tag ingredient rejects other items");
    helper.assertValueEqual(diamond.level(), 3, "plain field");

    EnchantedInput iron = ReloadTestContent.ITEM_DATA.get(Items.IRON_SWORD);
    helper.assertTrue(iron != null && iron.enchantment().is(Enchantments.KNOCKBACK), "redirect to a shared file in another namespace, got " + iron);
    helper.assertTrue(ReloadTestContent.ITEM_DATA.get(Items.STICK) == null, "items without a file have no data");
    helper.succeed();
  }

  @GameTest
  public void blockStateDataMapParsesWithWorldRegistries(GameTestHelper helper) {
    BlockState furnace = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
    EnchantedInput lit = ReloadTestContent.STATE_DATA.get(furnace.setValue(BlockStateProperties.LIT, true));
    helper.assertTrue(lit != null && lit.enchantment().is(Enchantments.FIRE_ASPECT), "inline variant value, got " + lit);
    helper.assertTrue(lit.input().test(new ItemStack(Items.OAK_LOG)), "tag ingredient inside a variant");
    EnchantedInput unlit = ReloadTestContent.STATE_DATA.get(furnace.setValue(BlockStateProperties.LIT, false));
    helper.assertTrue(unlit != null && unlit.enchantment().is(Enchantments.UNBREAKING), "variant redirected to a shared file, got " + unlit);
    helper.assertTrue(ReloadTestContent.STATE_DATA.get(Blocks.STONE.defaultBlockState()) == null, "blocks without a file have no data");
    helper.succeed();
  }

  @GameTest
  public void mergingLoaderMergesEveryPackWithRegistries(GameTestHelper helper) {
    helper.assertTrue(ReloadTestContent.MERGED.hadRegistries(), "merging loader context holds the registries");
    MergedEntry sample = ReloadTestContent.MERGED.get(MERGED_SAMPLE);
    helper.assertTrue(sample != null, "merged file loaded");
    helper.assertValueEqual(sample.values.stream().sorted().toList(), List.of("base", "overlay"), "one value per pack holding the file");
    helper.assertTrue(sample.enchantment != null && sample.enchantment.is(Enchantments.SHARPNESS), "enchantment parsed with the context, got " + sample.enchantment);
    helper.succeed();
  }

  @GameTest(maxTicks = RELOAD_TICKS)
  public void reloadRebuildsDataWithWorldRegistries(GameTestHelper helper) {
    MinecraftServer server = helper.getLevel().getServer();
    int loadsBefore = ReloadTestContent.MERGED.loadCount();
    EnchantedInput before = ReloadTestContent.ITEM_DATA.get(Items.DIAMOND_SWORD);
    CompletableFuture<Void> reload = server.reloadResources(server.getPackRepository().getSelectedIds());

    helper.succeedWhen(() -> {
      helper.assertTrue(reload.isDone(), "data reload still running");
      helper.assertFalse(reload.isCompletedExceptionally(), "data reload failed, see the log");
      helper.assertValueEqual(ReloadTestContent.MERGED.loadCount(), loadsBefore + 1, "merging loader ran once more");
      helper.assertTrue(ReloadTestContent.MERGED.hadRegistries(), "reloaded context holds the registries");
      EnchantedInput after = ReloadTestContent.ITEM_DATA.get(Items.DIAMOND_SWORD);
      helper.assertTrue(after != null && after != before, "data map was rebuilt, not kept");
      helper.assertTrue(after.enchantment().is(Enchantments.SHARPNESS), "enchantment resolved again, got " + after.enchantment());
      helper.assertTrue(after.input().test(new ItemStack(Items.OAK_LOG)), "tag ingredient resolved against the reloaded tags");
    });
  }
}
