package slimeknights.mantle.data.loadable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.common.BlockStateLoadable;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.common.DisplayContextLoadable;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.data.loadable.common.Vector3fLoadable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slimeknights.mantle.data.loadable.LoadableTesting.builtInBuffer;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks loadables for game types against the built-in registries */
class CommonLoadableTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void registryEntriesUseIdsAndNetworkIds() {
    assertSame(Items.STICK, Loadables.ITEM.convert(json("\"minecraft:stick\""), "item"));
    assertSame(Items.STICK, network(Loadables.ITEM, Items.STICK));
    JsonSyntaxException missing = assertThrows(JsonSyntaxException.class, () -> Loadables.ITEM.convert(json("\"mantle:nope\""), "item"));
    assertTrue(missing.getMessage().contains("does not contain ID mantle:nope"), missing.getMessage());
    assertThrows(JsonSyntaxException.class, () -> Loadables.NON_EMPTY_ITEM.convert(json("\"minecraft:air\""), "item"));
    assertEquals("minecraft:logs", Loadables.ITEM_TAG.getString(Loadables.ITEM_TAG.parseString("minecraft:logs", "tag")));
  }

  // ItemStack needs item components, which 26.x binds only when a world loads data: stacks are tested in LoadableGameTest

  @Test
  void emptyStacksAreRejectedWithoutCreatingOne() {
    assertThrows(JsonSyntaxException.class, () -> ItemStackLoadable.REQUIRED_STACK.convert(json("{\"item\": \"minecraft:air\"}"), "result"));
    assertThrows(JsonSyntaxException.class, () -> ItemStackLoadable.REQUIRED_ITEM.convert(json("\"minecraft:air\""), "result"));
  }

  @Test
  void ingredientsUseTheVanillaFormat() {
    Ingredient sticks = IngredientLoadable.DISALLOW_EMPTY.convert(json("[\"minecraft:stick\", \"minecraft:apple\"]"), "input");
    assertEquals(json("[\"minecraft:stick\", \"minecraft:apple\"]"), IngredientLoadable.DISALLOW_EMPTY.serialize(sticks));
    Ingredient copied = network(IngredientLoadable.DISALLOW_EMPTY, sticks, builtInBuffer());
    assertEquals(IngredientLoadable.DISALLOW_EMPTY.serialize(sticks), IngredientLoadable.DISALLOW_EMPTY.serialize(copied));
    assertEquals(Optional.empty(), IngredientLoadable.OPTIONAL.convert(json("[]"), "input"));
    assertEquals(json("[]"), IngredientLoadable.OPTIONAL.serialize(Optional.empty()));
    assertTrue(IngredientLoadable.OPTIONAL.convert(json("\"minecraft:stick\""), "input").isPresent());
  }

  @Test
  void blockStatesReadPropertiesAndWriteDifferences() {
    BlockState xLog = BlockStateLoadable.DIFFERENCE.convert(json("{\"block\": \"minecraft:oak_log\", \"properties\": {\"axis\": \"x\"}}"), "state");
    assertEquals(Direction.Axis.X, xLog.getValue(RotatedPillarBlock.AXIS));
    assertEquals(xLog, json(BlockStateLoadable.DIFFERENCE, xLog));
    assertEquals(json("\"minecraft:oak_log\""), BlockStateLoadable.DIFFERENCE.serialize(Blocks.OAK_LOG.defaultBlockState()));
    assertTrue(BlockStateLoadable.ALL.serialize(Blocks.OAK_LOG.defaultBlockState()).isJsonObject());
    assertEquals(xLog, network(BlockStateLoadable.ALL, xLog));
    assertThrows(JsonSyntaxException.class, () -> BlockStateLoadable.ALL.convert(json("{\"block\": \"minecraft:oak_log\", \"properties\": {\"axis\": \"w\"}}"), "state"));
  }

  @Test
  void nbtReadsObjectsAndSnbtStrings() {
    CompoundTag fromString = NBTLoadable.ALLOW_STRING.convert(json("\"{Level:2b}\""), "nbt");
    assertEquals(2, fromString.getByteOr("Level", (byte) 0));
    assertThrows(JsonSyntaxException.class, () -> NBTLoadable.DISALLOW_STRING.convert(json("\"{Level:2b}\""), "nbt"));
    assertEquals(fromString, network(NBTLoadable.ALLOW_STRING, fromString));
  }

  @Test
  void colorsVectorsDisplayContextsAndRotations() {
    assertEquals(0x80FF0000, ColorLoadable.ALPHA.parseString("80FF0000", "color"));
    assertEquals(0xFFFF0000, ColorLoadable.NO_ALPHA.parseString("FF0000", "color"));
    assertEquals("FF0000", ColorLoadable.NO_ALPHA.getString(0xFFFF0000));
    assertThrows(JsonSyntaxException.class, () -> ColorLoadable.NO_ALPHA.parseString("80FF0000", "color"));
    // upstream wrote zero axes as missing keys but then required every key when reading
    Vector3f vector = new Vector3f(1, 0, 2);
    JsonObject object = new JsonObject();
    Vector3fLoadable.INSTANCE.serialize(vector, object);
    assertEquals(vector, Vector3fLoadable.INSTANCE.convert(object, "offset"));
    assertEquals(vector, json(Vector3fLoadable.INSTANCE, vector));
    assertEquals(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, DisplayContextLoadable.INSTANCE.parseString("thirdperson_righthand", "context"));
    assertEquals(ItemDisplayContext.GUI, DisplayContextLoadable.INSTANCE.parseString("minecraft:gui", "context"));
    assertEquals(ItemDisplayContext.GUI, network(DisplayContextLoadable.INSTANCE, ItemDisplayContext.GUI));
    assertThrows(JsonSyntaxException.class, () -> Loadables.ROTATION.convert(json("45"), "rotation"));
  }

  @Test
  void datapackRegistriesNeedWorldRegistries() {
    JsonSyntaxException error = assertThrows(JsonSyntaxException.class, () -> Loadables.ENCHANTMENT.convert(json("\"minecraft:sharpness\""), "enchantment"));
    assertTrue(error.getMessage().contains("needs world registries"), error.getMessage());
  }

  @Test
  void lootEntriesUseTheVanillaCodec() {
    var entry = Loadables.LOOT_ENTRY.convert(json("{\"type\": \"minecraft:item\", \"name\": \"minecraft:stick\"}"), "entry");
    assertEquals(json("{\"type\": \"minecraft:item\", \"name\": \"minecraft:stick\"}"), Loadables.LOOT_ENTRY.serialize(entry));
  }
}
