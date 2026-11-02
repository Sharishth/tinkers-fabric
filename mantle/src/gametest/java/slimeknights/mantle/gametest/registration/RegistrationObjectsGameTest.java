package slimeknights.mantle.gametest.registration;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import slimeknights.mantle.block.MantleStairBlock;
import slimeknights.mantle.gametest.registration.RegistrationTestContent.Variant;
import slimeknights.mantle.registration.deferred.PotionDeferredRegister.PotionType;

import java.util.HashSet;
import java.util.List;

import static slimeknights.mantle.gametest.registration.RegistrationTestContent.BUILDING;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.EXTENDED_MENU;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.FENCE_BUILDING;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.METAL;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.NAMESPACE;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.NO_ITEM_BLOCK;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.PLAIN_BLOCK;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.PLAIN_MENU;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.TEST_ATTRIBUTE;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.TEST_BLOCK_ENTITY;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.TEST_POTIONS;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.TEST_SOUNDS;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.TEST_ZOMBIE;
import static slimeknights.mantle.gametest.registration.RegistrationTestContent.WALL_BUILDING;

/** Checks Mantle's specialised registers and objects against the real game registries. */
public class RegistrationObjectsGameTest {
  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(NAMESPACE, path);
  }

  @GameTest
  public void buildingBlocksHaveAllVariants(GameTestHelper helper) {
    helper.assertTrue(BUILDING.getSlab() instanceof SlabBlock, "slab type");
    helper.assertTrue(BUILDING.getStairs() instanceof MantleStairBlock stairs && stairs.getBaseState() == BUILDING.get().defaultBlockState(), "stairs copy the base block");
    helper.assertTrue(WALL_BUILDING.getWall() instanceof WallBlock, "wall type");
    helper.assertTrue(FENCE_BUILDING.getFence() instanceof FenceBlock, "fence type");
    for (String item : new String[] {"test_brick_slab", "test_brick_stairs", "test_stone_wall", "test_plank_fence"}) {
      helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(item)), "missing item " + item);
    }
    helper.assertTrue(WALL_BUILDING.values().size() == 4 && FENCE_BUILDING.values().size() == 4, "4 blocks per wall/fence object");
    helper.succeed();
  }

  @GameTest
  public void metalObjectHasBlockIngotNuggetAndTags(GameTestHelper helper) {
    helper.assertTrue(METAL.getId().equals(id("test_metal_block")), "block id " + METAL.getId());
    helper.assertTrue(METAL.getIngot() == BuiltInRegistries.ITEM.getValue(id("test_metal_ingot")), "ingot registered");
    helper.assertTrue(METAL.getNugget() == BuiltInRegistries.ITEM.getValue(id("test_metal_nugget")), "nugget registered");
    helper.assertTrue(METAL.getIngotTag().location().equals(Identifier.fromNamespaceAndPath("c", "ingots/test_metal")), "ingot tag " + METAL.getIngotTag());
    helper.assertTrue(METAL.getBlockTag().location().equals(Identifier.fromNamespaceAndPath("c", "storage_blocks/test_metal")), "block tag " + METAL.getBlockTag());
    List<ItemLike> values = METAL.values();
    helper.assertTrue(new HashSet<>(values).size() == 3 && values.contains(METAL.getNugget()), "values lists block, ingot and nugget once each");
    helper.succeed();
  }

  @GameTest
  public void entityTypeAndSpawnEggRegistered(GameTestHelper helper) {
    helper.assertTrue(TEST_ZOMBIE.get() == BuiltInRegistries.ENTITY_TYPE.getValue(id("test_zombie")), "entity type registered");
    Item egg = TEST_ZOMBIE.asItem();
    helper.assertTrue(egg instanceof SpawnEggItem && egg == BuiltInRegistries.ITEM.getValue(id("test_zombie_spawn_egg")), "spawn egg registered");
    helper.succeed();
  }

  @GameTest
  public void blockEntityTypeAcceptsItsBlock(GameTestHelper helper) {
    helper.assertTrue(TEST_BLOCK_ENTITY.get().isValid(PLAIN_BLOCK.get().defaultBlockState()), "valid for plain block");
    helper.assertTrue(!TEST_BLOCK_ENTITY.get().isValid(NO_ITEM_BLOCK.get().defaultBlockState()), "not valid for other blocks");
    helper.succeed();
  }

  @GameTest
  public void menuTypesRegistered(GameTestHelper helper) {
    helper.assertTrue(PLAIN_MENU.get() == BuiltInRegistries.MENU.getValue(id("test_menu")), "plain menu registered");
    helper.assertTrue(EXTENDED_MENU.get() instanceof ExtendedMenuType<?, ?> && EXTENDED_MENU.get() == BuiltInRegistries.MENU.getValue(id("test_extended_menu")), "extended menu registered");
    helper.succeed();
  }

  @GameTest
  public void attributeHasHolderAndDefault(GameTestHelper helper) {
    helper.assertTrue(TEST_ATTRIBUTE.getHolder().isPresent(), "attribute holder bound (needed for modifiers)");
    helper.assertTrue(TEST_ATTRIBUTE.get().getDefaultValue() == 1.0, "default value 1");
    helper.succeed();
  }

  @GameTest
  public void potionVariantsUseUpstreamFormulas(GameTestHelper helper) {
    helper.assertTrue(TEST_POTIONS.get(PotionType.NORMAL) == BuiltInRegistries.POTION.getValue(id("test_potion")), "normal id has no prefix");
    helper.assertTrue(TEST_POTIONS.get(PotionType.STRONG) == BuiltInRegistries.POTION.getValue(id("strong_test_potion")), "strong id");
    helper.assertTrue(TEST_POTIONS.get(PotionType.LONG) == BuiltInRegistries.POTION.getValue(id("long_test_potion")), "long id");
    MobEffectInstance strong = firstEffect(TEST_POTIONS.get(PotionType.STRONG));
    MobEffectInstance longer = firstEffect(TEST_POTIONS.get(PotionType.LONG));
    helper.assertTrue(strong.getAmplifier() == 1 && strong.getDuration() == 1800, "strong: +1 level, half duration");
    helper.assertTrue(longer.getAmplifier() == 0 && longer.getDuration() == 9600, "long: 8/3 duration");
    helper.succeed();
  }

  @GameTest
  public void enumRegisterSuffixesNames(GameTestHelper helper) {
    helper.assertTrue(TEST_SOUNDS.get(Variant.ALPHA) == BuiltInRegistries.SOUND_EVENT.getValue(id("test_sound_alpha")), "alpha sound");
    helper.assertTrue(TEST_SOUNDS.get(Variant.BETA) == BuiltInRegistries.SOUND_EVENT.getValue(id("test_sound_beta")), "beta sound");
    helper.succeed();
  }

  private static MobEffectInstance firstEffect(Potion potion) {
    return potion.getEffects().getFirst();
  }
}
