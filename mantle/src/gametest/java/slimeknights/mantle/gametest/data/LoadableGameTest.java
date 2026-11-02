package slimeknights.mantle.gametest.data;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

/** Checks loadables that need a world's registries: datapack registries, tags and enchanted stacks (D-21) */
public class LoadableGameTest {
  private static TypedMap worldContext(GameTestHelper helper) {
    return TypedMapBuilder.builder().put(ContextKey.REGISTRIES, helper.getLevel().registryAccess()).build();
  }

  private static RegistryFriendlyByteBuf worldBuffer(GameTestHelper helper) {
    RegistryAccess registries = helper.getLevel().registryAccess();
    return new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
  }

  @GameTest
  public void enchantmentHoldersResolveWithWorldRegistries(GameTestHelper helper) {
    Holder<Enchantment> sharpness = Loadables.ENCHANTMENT.convert(JsonParser.parseString("\"minecraft:sharpness\""), "enchantment", worldContext(helper));
    helper.assertTrue(sharpness.is(Enchantments.SHARPNESS), "parsed sharpness, got " + sharpness);
    RegistryFriendlyByteBuf buffer = worldBuffer(helper);
    Loadables.ENCHANTMENT.encode(buffer, sharpness);
    helper.assertTrue(Loadables.ENCHANTMENT.decode(buffer).is(Enchantments.SHARPNESS), "network round trip");
    helper.succeed();
  }

  @GameTest
  public void tagIngredientsMatchTaggedItems(GameTestHelper helper) {
    Ingredient logs = IngredientLoadable.DISALLOW_EMPTY.convert(JsonParser.parseString("\"#minecraft:logs\""), "input", worldContext(helper));
    helper.assertTrue(logs.test(new ItemStack(Items.OAK_LOG)), "oak log is in #logs");
    helper.assertFalse(logs.test(new ItemStack(Items.STICK)), "stick is not in #logs");
    helper.succeed();
  }

  @GameTest
  public void stacksReadIdsCountsAndComponents(GameTestHelper helper) {
    helper.assertTrue(ItemStackLoadable.REQUIRED_ITEM.convert(JsonParser.parseString("\"minecraft:stick\""), "result").is(Items.STICK), "plain id");
    ItemStack three = ItemStackLoadable.REQUIRED_STACK.convert(JsonParser.parseString("{\"item\": \"minecraft:stick\", \"count\": 3}"), "result");
    helper.assertValueEqual(three.getCount(), 3, "count");
    helper.assertValueEqual(ItemStackLoadable.REQUIRED_STACK.serialize(new ItemStack(Items.STICK)), JsonParser.parseString("\"minecraft:stick\""), "count 1 written as the id");
    ItemStack small = ItemStackLoadable.REQUIRED_STACK_COMPONENTS.convert(
      JsonParser.parseString("{\"item\": \"minecraft:stick\", \"count\": 2, \"components\": {\"minecraft:max_stack_size\": 16}}"), "result");
    helper.assertValueEqual(small.getMaxStackSize(), 16, "component applied");
    helper.assertTrue(ItemStackLoadable.REQUIRED_STACK_COMPONENTS.serialize(small).getAsJsonObject().has("components"), "components written");
    RegistryFriendlyByteBuf buffer = worldBuffer(helper);
    ItemStackLoadable.REQUIRED_STACK_COMPONENTS.encode(buffer, small);
    helper.assertTrue(ItemStack.matches(small, ItemStackLoadable.REQUIRED_STACK_COMPONENTS.decode(buffer)), "network keeps count and components");
    Ingredient sticks = IngredientLoadable.DISALLOW_EMPTY.convert(JsonParser.parseString("[\"minecraft:stick\", \"minecraft:apple\"]"), "input");
    helper.assertTrue(sticks.test(new ItemStack(Items.APPLE)) && !sticks.test(new ItemStack(Items.DIRT)), "item list ingredient");
    helper.succeed();
  }

  @GameTest
  public void enchantedStacksRoundTripComponents(GameTestHelper helper) {
    String json = "{\"item\": \"minecraft:diamond_sword\", \"components\": {\"minecraft:enchantments\": {\"minecraft:sharpness\": 3}}}";
    ItemStack sword = ItemStackLoadable.REQUIRED_ITEM_COMPONENTS.convert(JsonParser.parseString(json), "result", worldContext(helper));
    Holder<Enchantment> sharpness = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
    helper.assertValueEqual(sword.getEnchantments().getLevel(sharpness), 3, "sharpness level");
    RegistryFriendlyByteBuf buffer = worldBuffer(helper);
    ItemStackLoadable.REQUIRED_ITEM_COMPONENTS.encode(buffer, sword);
    helper.assertTrue(ItemStack.matches(sword, ItemStackLoadable.REQUIRED_ITEM_COMPONENTS.decode(buffer)), "network keeps enchantments");
    helper.succeed();
  }
}
