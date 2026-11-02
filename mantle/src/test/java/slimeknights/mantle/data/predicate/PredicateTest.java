package slimeknights.mantle.data.predicate;

import com.google.gson.JsonSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.mantle.data.predicate.block.BlockPropertiesPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks predicate registries and the block, item and fluid predicates */
class PredicateTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    MantlePredicates.register();
  }

  private static IJsonPredicate<BlockState> block(String json) {
    return BlockPredicate.LOADER.convert(json(json), "predicate");
  }

  @Test
  void sharedTypesComposePredicates() {
    assertSame(BlockPredicate.ANY, BlockPredicate.LOADER.convert(json("null"), "predicate"));
    assertSame(BlockPredicate.ANY, block("\"mantle:any\""));
    IJsonPredicate<BlockState> stoneOrDirt = block("{\"type\": \"mantle:or\", \"predicates\": ["
      + "{\"type\": \"mantle:set\", \"blocks\": [\"minecraft:stone\"]}, {\"type\": \"mantle:set\", \"blocks\": [\"minecraft:dirt\"]}]}");
    assertTrue(stoneOrDirt.matches(Blocks.DIRT.defaultBlockState()));
    assertFalse(stoneOrDirt.matches(Blocks.SAND.defaultBlockState()));
    IJsonPredicate<BlockState> notStone = block("{\"type\": \"mantle:inverted\", \"inverted_type\": \"mantle:set\", \"blocks\": [\"minecraft:stone\"]}");
    assertTrue(notStone.matches(Blocks.SAND.defaultBlockState()));
    assertSame(stoneOrDirt, stoneOrDirt.inverted().inverted());
    assertThrows(JsonSyntaxException.class, () -> block("{\"type\": \"mantle:and\", \"predicates\": [\"mantle:any\"]}"), "and needs 2 children");
  }

  @Test
  void predicatesRoundTripJsonAndNetwork() {
    IJsonPredicate<BlockState> and = BlockPredicate.and(BlockPredicate.set(Blocks.STONE), BlockPredicate.REQUIRES_TOOL);
    IJsonPredicate<BlockState> read = BlockPredicate.LOADER.convert(BlockPredicate.LOADER.serialize(and), "predicate");
    assertTrue(read.matches(Blocks.STONE.defaultBlockState()));
    assertFalse(read.matches(Blocks.DIRT.defaultBlockState()));
    assertTrue(network(BlockPredicate.LOADER, and).matches(Blocks.STONE.defaultBlockState()));
    assertEquals(json("\"mantle:requires_tool\""), BlockPredicate.LOADER.serialize(BlockPredicate.REQUIRES_TOOL));
    assertSame(BlockPredicate.ANY, network(BlockPredicate.LOADER, BlockPredicate.ANY));
  }

  @Test
  void blockPropertiesMatchValuesListsAndRanges() {
    IJsonPredicate<BlockState> xLog = block("{\"type\": \"mantle:block_properties\", \"block\": \"minecraft:oak_log\", \"properties\": {\"axis\": \"x\"}}");
    assertTrue(xLog.matches(Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
    assertFalse(xLog.matches(Blocks.OAK_LOG.defaultBlockState()));
    IJsonPredicate<BlockState> ripe = block("{\"type\": \"mantle:block_properties\", \"block\": \"minecraft:wheat\", \"properties\": {\"age\": {\"min\": \"5\"}}}");
    assertTrue(ripe.matches(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7)));
    assertFalse(ripe.matches(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 4)));
    BlockPropertiesPredicate built = BlockPropertiesPredicate.block(Blocks.WHEAT).range(CropBlock.AGE, 2, 3).build();
    IJsonPredicate<BlockState> copied = network(BlockPredicate.LOADER, built);
    assertTrue(copied.matches(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3)));
    assertFalse(copied.matches(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 4)));
    assertTrue(BlockPredicate.LOADER.convert(BlockPredicate.LOADER.serialize(built), "p").matches(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 2)));
    assertThrows(JsonSyntaxException.class, () -> block("{\"type\": \"mantle:block_properties\", \"block\": \"minecraft:stone\", \"properties\": {\"axis\": \"x\"}}"));
  }

  @Test
  void itemAndFluidSingletons() {
    assertTrue(ItemPredicate.HAS_CONTAINER.matches(Items.WATER_BUCKET));
    assertFalse(ItemPredicate.HAS_CONTAINER.matches(Items.STICK));
    assertTrue(ItemPredicate.LOADER.convert(json("{\"type\": \"mantle:set\", \"items\": [\"minecraft:stick\"]}"), "p").matches(Items.STICK));
    assertTrue(FluidPredicate.SOURCE.matches(Fluids.WATER));
    assertFalse(FluidPredicate.SOURCE.matches(Fluids.FLOWING_WATER));
    assertTrue(FluidPredicate.HAS_BUCKET.matches(Fluids.LAVA));
    assertSame(FluidPredicate.LIGHTER_THAN_AIR, FluidPredicate.LOADER.convert(json("\"mantle:lighter_than_air\""), "p"));
    assertFalse(FluidPredicate.LIGHTER_THAN_AIR.matches(Fluids.FLOWING_WATER), "flowing fluids use their source");
  }

  @Test
  void fallbackRegistryAcceptsTheOtherRegistrysTypes() {
    FallbackPredicateRegistry<BlockState, Item> blocksByItem = new FallbackPredicateRegistry<>(
      "Test Predicate", BlockPredicate.ANY, BlockPredicate.NONE, ItemPredicate.LOADER, state -> state.getBlock().asItem(), "item");
    IJsonPredicate<BlockState> stoneItem = blocksByItem.convert(json("{\"type\": \"mantle:set\", \"items\": [\"minecraft:stone\"]}"), "p");
    assertTrue(stoneItem.matches(Blocks.STONE.defaultBlockState()));
    assertFalse(stoneItem.matches(Blocks.DIRT.defaultBlockState()));
    assertEquals(json("{\"type\": \"mantle:set\", \"items\": [\"minecraft:stone\"]}"), blocksByItem.serialize(stoneItem), "fallbacks are written unwrapped");
    assertSame(BlockPredicate.ANY, blocksByItem.convert(json("\"mantle:any\""), "p"), "own types win over the fallback");
  }
}
