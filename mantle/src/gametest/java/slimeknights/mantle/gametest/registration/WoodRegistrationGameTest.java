package slimeknights.mantle.gametest.registration;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import slimeknights.mantle.block.MantleButtonBlock;
import slimeknights.mantle.block.MantleDoorBlock;
import slimeknights.mantle.block.MantlePressurePlateBlock;
import slimeknights.mantle.block.MantleTrapDoorBlock;
import slimeknights.mantle.registration.object.WoodBlockObject;

import java.util.HashSet;
import java.util.List;

import static slimeknights.mantle.gametest.registration.RegistrationTestContent.FIREPROOF_WOOD;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.FLAMMABLE_WOOD;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.NAMESPACE;

/** Checks {@code BlockDeferredRegister.registerWood} against the real game: ids, types, signs, stripping and fuel. */
public class WoodRegistrationGameTest {
  /** Block ids registered for a wood named {@code test_wood}; the placeholder is replaced by the wood name */
  private static final List<String> BLOCK_IDS = List.of(
    "#_planks", "#_planks_slab", "#_planks_stairs", "#_fence", "#_fence_gate",
    "#_log", "stripped_#_log", "#_wood", "stripped_#_wood",
    "#_door", "#_trapdoor", "#_pressure_plate", "#_button",
    "#_sign", "#_wall_sign", "#_hanging_sign", "#_wall_hanging_sign");
  /** Wall signs share the item of their standing sign */
  private static final List<String> BLOCKS_WITHOUT_ITEMS = List.of("#_wall_sign", "#_wall_hanging_sign");

  private static Identifier id(String pattern, String woodName) {
    return Identifier.fromNamespaceAndPath(NAMESPACE, pattern.replace("#", woodName));
  }

  @GameTest
  public void woodRegistersEveryBlockAndItem(GameTestHelper helper) {
    for (String pattern : BLOCK_IDS) {
      helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(pattern, "test_wood")), "missing block " + pattern);
      boolean hasItem = BuiltInRegistries.ITEM.containsKey(id(pattern, "test_wood"));
      helper.assertTrue(hasItem != BLOCKS_WITHOUT_ITEMS.contains(pattern), "item presence wrong for " + pattern);
    }
    List<Block> values = FLAMMABLE_WOOD.values();
    helper.assertTrue(values.size() == BLOCK_IDS.size() && new HashSet<>(values).size() == BLOCK_IDS.size(), "values lists 17 distinct blocks");
    helper.assertTrue(FLAMMABLE_WOOD.getLogBlockTag().location().equals(id("#_logs", "test_wood")), "log tag " + FLAMMABLE_WOOD.getLogBlockTag());
    helper.succeed();
  }

  @GameTest
  public void woodUsesMantleBlocksAndVanillaItemShapes(GameTestHelper helper) {
    helper.assertTrue(FLAMMABLE_WOOD.getDoor() instanceof MantleDoorBlock, "door type");
    helper.assertTrue(FLAMMABLE_WOOD.getTrapdoor() instanceof MantleTrapDoorBlock, "trapdoor type");
    helper.assertTrue(FLAMMABLE_WOOD.getPressurePlate() instanceof MantlePressurePlateBlock, "pressure plate type");
    helper.assertTrue(FLAMMABLE_WOOD.getButton() instanceof MantleButtonBlock, "button type");
    helper.assertTrue(FLAMMABLE_WOOD.getLog() instanceof RotatedPillarBlock, "log type");
    helper.assertTrue(FLAMMABLE_WOOD.getDoor().asItem() instanceof DoubleHighBlockItem, "door item places both halves");
    Item sign = FLAMMABLE_WOOD.getSign().asItem();
    helper.assertTrue(sign instanceof StandingAndWallBlockItem && FLAMMABLE_WOOD.getWallSign().asItem() == sign, "wall sign shares the sign item");
    Item hangingSign = FLAMMABLE_WOOD.getHangingSign().asItem();
    helper.assertTrue(hangingSign instanceof HangingSignItem && FLAMMABLE_WOOD.getWallHangingSign().asItem() == hangingSign, "wall hanging sign shares the item");
    helper.assertTrue(sign.getDefaultMaxStackSize() == 16 && sign.components().has(DataComponents.SIGN_TEXT_FRONT), "sign item stacks to 16 with text");
    helper.succeed();
  }

  @GameTest
  public void woodTypeRegisteredWithNamespacedName(GameTestHelper helper) {
    WoodType type = FLAMMABLE_WOOD.getWoodType();
    helper.assertValueEqual(type.name(), NAMESPACE + ":test_wood", "wood type name");
    helper.assertTrue(WoodType.values().anyMatch(type::equals), "wood type listed in WoodType.values()");
    helper.assertTrue(FLAMMABLE_WOOD.getSign().type() == type && FLAMMABLE_WOOD.getWallHangingSign().type() == type, "signs use the wood type");
    helper.assertTrue(FLAMMABLE_WOOD.getDoor().type() == type.setType(), "door uses the wood's block set");
    helper.succeed();
  }

  @GameTest
  public void wallSignsDropAndNameAsStandingSigns(GameTestHelper helper) {
    WoodBlockObject wood = FLAMMABLE_WOOD;
    helper.assertTrue(wood.getWallSign().getLootTable().equals(wood.getSign().getLootTable()), "wall sign loot table");
    helper.assertTrue(wood.getWallSign().getDescriptionId().equals(wood.getSign().getDescriptionId()), "wall sign name");
    helper.assertTrue(wood.getWallHangingSign().getLootTable().equals(wood.getHangingSign().getLootTable()), "wall hanging sign loot table");
    helper.succeed();
  }

  @GameTest
  public void signsUseVanillaBlockEntityTypes(GameTestHelper helper) {
    WoodBlockObject wood = FLAMMABLE_WOOD;
    helper.assertTrue(BlockEntityTypes.SIGN.isValid(wood.getSign().defaultBlockState()), "sign accepted");
    helper.assertTrue(BlockEntityTypes.SIGN.isValid(wood.getWallSign().defaultBlockState()), "wall sign accepted");
    helper.assertTrue(BlockEntityTypes.HANGING_SIGN.isValid(wood.getHangingSign().defaultBlockState()), "hanging sign accepted");
    helper.assertTrue(BlockEntityTypes.HANGING_SIGN.isValid(wood.getWallHangingSign().defaultBlockState()), "wall hanging sign accepted");
    BlockPos signPos = new BlockPos(0, 1, 0);
    BlockPos hangingPos = new BlockPos(1, 1, 0);
    helper.setBlock(signPos, wood.getSign());
    helper.setBlock(hangingPos, wood.getHangingSign());
    // getBlockEntity fails the test itself when the entity is missing or of another class
    helper.getBlockEntity(signPos, SignBlockEntity.class);
    helper.getBlockEntity(hangingPos, HangingSignBlockEntity.class);
    helper.succeed();
  }

  @GameTest
  public void axeStripsLogsAndWoodKeepingAxis(GameTestHelper helper) {
    assertStrips(helper, new BlockPos(0, 1, 0), FLAMMABLE_WOOD.getLog(), FLAMMABLE_WOOD.getStrippedLog());
    assertStrips(helper, new BlockPos(1, 1, 0), FLAMMABLE_WOOD.getWood(), FLAMMABLE_WOOD.getStrippedWood());
    helper.succeed();
  }

  /** Places the block lying on the X axis, uses an axe on it, and checks the stripped block kept the axis */
  private static void assertStrips(GameTestHelper helper, BlockPos pos, Block block, Block stripped) {
    helper.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
    helper.useBlock(pos, player);
    BlockState expected = stripped.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    helper.assertBlockState(pos, expected);
  }

  @GameTest
  public void flammableWoodBurnsLikeOak(GameTestHelper helper) {
    WoodBlockObject wood = FLAMMABLE_WOOD;
    assertSameFuel(helper, wood, Items.OAK_PLANKS);
    assertSameFuel(helper, wood.getSlab(), Items.OAK_SLAB);
    assertSameFuel(helper, wood.getLog(), Items.OAK_LOG);
    assertSameFuel(helper, wood.getDoor(), Items.OAK_DOOR);
    assertSameFuel(helper, wood.getButton(), Items.OAK_BUTTON);
    assertSameFuel(helper, wood.getSign(), Items.OAK_SIGN);
    assertSameFuel(helper, wood.getHangingSign(), Items.OAK_HANGING_SIGN);
    helper.succeed();
  }

  @GameTest
  public void fireproofWoodIsNotFuel(GameTestHelper helper) {
    for (Block block : FIREPROOF_WOOD.values()) {
      Item item = block.asItem();
      helper.assertTrue(!item.components().has(DataComponents.COOKING_FUEL), "fireproof " + BuiltInRegistries.ITEM.getKey(item) + " burns");
    }
    helper.succeed();
  }

  private static void assertSameFuel(GameTestHelper helper, ItemLike ours, Item vanilla) {
    Item item = ours.asItem();
    helper.assertTrue(vanilla.components().has(DataComponents.COOKING_FUEL), "vanilla " + vanilla + " should be fuel");
    helper.assertValueEqual(item.components().get(DataComponents.COOKING_FUEL), vanilla.components().get(DataComponents.COOKING_FUEL),
                            "fuel of " + BuiltInRegistries.ITEM.getKey(item));
  }
}
