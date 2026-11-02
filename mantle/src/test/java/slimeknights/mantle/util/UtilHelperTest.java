package slimeknights.mantle.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the plain util helpers; ItemStackList needs item components, so it is tested in a GameTest */
class UtilHelperTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void jsonHelperParsesListsPathsAndCodecs() {
    JsonArray array = new JsonArray();
    array.add(1);
    array.add(2);
    assertEquals(List.of(1, 2), JsonHelper.parseList(array, "numbers", IntLoadable.FROM_ZERO));
    JsonSyntaxException empty = assertThrows(JsonSyntaxException.class, () -> JsonHelper.parseList(new JsonArray(), "numbers", IntLoadable.FROM_ZERO));
    assertEquals("numbers must have at least 1 element", empty.getMessage());
    Identifier recipe = Identifier.fromNamespaceAndPath("mantle", "recipes/tool/pick.json");
    assertEquals(Identifier.fromNamespaceAndPath("mantle", "tool/pick"), JsonHelper.localize(recipe, "recipes", ".json"));
    assertEquals(Identifier.fromNamespaceAndPath("mantle", "a/x_b"), JsonHelper.wrap(Identifier.fromNamespaceAndPath("mantle", "x"), "a/", "_b"));
    assertEquals(5, JsonHelper.parse(Codec.INT, new JsonPrimitive(5)));
    assertEquals(new JsonPrimitive(5), JsonHelper.serialize(Codec.INT, 5));
    assertEquals("\"mantle:x\"", JsonHelper.DEFAULT_GSON.toJson(Identifier.fromNamespaceAndPath("mantle", "x")));
  }

  @Test
  void registryHelperFindsRegistries() {
    assertSame(BuiltInRegistries.BLOCK, RegistryHelper.<Block>getRegistry(Registries.BLOCK));
    assertNull(RegistryHelper.getRegistry(net.minecraft.resources.ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("mantle", "missing"))));
  }

  @Test
  void logicAndTranslationHelpers() {
    assertEquals(7, LogicHelper.defaultIf(-1, -1, 7));
    assertEquals(3, LogicHelper.defaultIf(3, -1, 7));
    assertEquals("fallback", LogicHelper.getOrDefault(List.of("a"), 4, "fallback"));
    assertTrue(LogicHelper.isInList(new String[] {"a", "b"}, "b"));
    assertEquals("line one\nline two", TranslationHelper.convertNewlines("line one\\nline two"));
    assertNull(TranslationHelper.convertNewlines(null));
  }

  @Test
  void reversedListBuilderHandsOutLastFirst() {
    ReversedListBuilder<List<String>> builder = new ReversedListBuilder<>();
    builder.add(List.of("low"));
    builder.add(List.of("high", "higher"));
    assertEquals(3, ReversedListBuilder.size(builder));
    assertEquals(List.of("high", "higher", "low"), ReversedListBuilder.buildList(builder));
    List<List<String>> order = new ArrayList<>();
    builder.build(order::add);
    assertEquals(List.of("high", "higher"), order.getFirst());
  }
}
