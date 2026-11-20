package slimeknights.mantle.data.datamap;

import com.google.gson.JsonElement;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;

/** Checks data map loaders, variant keys and providers without a world; parsing with world registries is a GameTest */
class DataMapTest {
  private static final String FOLDER = "test/glow";

  record Glow(int light, String color) {}

  static final RecordLoadable<Glow> GLOW = RecordLoadable.create(
    IntLoadable.FROM_ZERO.requiredField("light", Glow::light),
    StringLoadable.DEFAULT.defaultField("color", "none", Glow::color),
    Glow::new);

  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  private static Identifier id(String text) {
    return Identifier.parse(text);
  }

  private static Map<Identifier, JsonElement> jsons(String... idsAndJson) {
    Map<Identifier, JsonElement> jsons = new HashMap<>();
    for (int i = 0; i < idsAndJson.length; i += 2) {
      jsons.put(id(idsAndJson[i]), json(idsAndJson[i + 1]));
    }
    return jsons;
  }

  @Test
  void variantKeysAreSortedAndValidated() {
    StateVariantStringBuilder builder = new StateVariantStringBuilder(Blocks.OAK_STAIRS)
      .when(BlockStateProperties.HALF, Half.TOP)
      .when(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
    assertEquals("facing=north,half=top", builder.toString());
    assertEquals("", new StateVariantStringBuilder(Blocks.STONE).toString());
    assertThrows(IllegalArgumentException.class, () -> builder.when(BlockStateProperties.HALF, Half.BOTTOM), "property set twice");
    assertThrows(IllegalArgumentException.class, () -> builder.when(BlockStateProperties.LIT, true), "stairs are never lit");
  }

  @Test
  void variantPredicatesMatchLikeBlockStateFiles() {
    var stairs = Blocks.OAK_STAIRS.getStateDefinition();
    Predicate<BlockState> northTop = StateVariantStringBuilder.predicate(stairs, "facing=north,half=top");
    // 5 shapes x 2 waterlogged values are left free
    assertEquals(10, stairs.getPossibleStates().stream().filter(northTop).count());
    assertEquals(stairs.getPossibleStates().size(), stairs.getPossibleStates().stream().filter(StateVariantStringBuilder.predicate(stairs, "")).count());
    assertFalse(northTop.test(Blocks.STONE.defaultBlockState()), "other blocks never match");
    assertThrows(IllegalArgumentException.class, () -> StateVariantStringBuilder.predicate(stairs, "lit=true"));
    assertThrows(IllegalArgumentException.class, () -> StateVariantStringBuilder.predicate(stairs, "half=middle"));
  }

  @Test
  void registryLoaderResolvesParentsAndSkipsBrokenFiles() {
    RegistryDataMapLoader<Block, Glow> loader = new RegistryDataMapLoader<>("Test glow", PackType.SERVER_DATA, FOLDER, BuiltInRegistries.BLOCK, GLOW);
    loader.apply(jsons(
      "minecraft:stone", "{\"light\": 3, \"color\": \"red\"}",
      "mantle:shared", "{\"light\": 7}",
      "minecraft:dirt", "{\"parent\": \"mantle:shared\"}",
      "minecraft:granite", "{\"parent\": \"mantle:shared\"}",
      "minecraft:sand", "{\"parent\": \"minecraft:stone\", \"light\": 9}",
      "minecraft:gravel", "{}",
      "minecraft:clay", "{\"parent\": \"minecraft:clay\", \"light\": 1}",
      "minecraft:glass", "{\"parent\": \"mantle:missing\"}"
    ), ResourceManager.Empty.INSTANCE, TypedMap.EMPTY);

    assertEquals(new Glow(3, "red"), loader.get(Blocks.STONE));
    assertEquals(new Glow(7, "none"), loader.get(Blocks.DIRT));
    assertSame(loader.get(Blocks.DIRT), loader.get(Blocks.GRANITE), "redirects to one file share the parsed value");
    assertEquals(new Glow(9, "red"), loader.get(Blocks.SAND), "child keys win, missing keys come from the parent");
    assertNull(loader.get(Blocks.GRAVEL), "empty object disables the entry");
    assertNull(loader.get(Blocks.CLAY), "circular parent is an error");
    assertNull(loader.get(Blocks.GLASS), "missing parent is an error");
    Glow fallback = new Glow(0, "fallback");
    assertSame(fallback, loader.get(Blocks.GLASS, fallback));
  }

  @Test
  void blockStateLoaderReadsVariantsAndRedirects() {
    BlockStateDataMapLoader<Glow> loader = new BlockStateDataMapLoader<>("Test light", PackType.CLIENT_RESOURCES, "test/light", GLOW);
    loader.apply(jsons(
      "minecraft:furnace", "{\"variants\": {\"lit=true\": {\"light\": 13}, \"lit=false\": \"mantle:dark\"}}",
      "mantle:dark", "{\"light\": 0, \"color\": \"black\"}",
      "minecraft:oak_stairs", "{\"variants\": {\"\": {\"light\": 4}}}",
      "minecraft:lever", "{\"variants\": {\"powered=true\": {\"light\": 1}, \"bogus=x\": {\"light\": 2}}}",
      "minecraft:stone", "{}"
    ), ResourceManager.Empty.INSTANCE, TypedMap.EMPTY);

    BlockState furnace = Blocks.FURNACE.defaultBlockState();
    assertEquals(new Glow(13, "none"), loader.get(furnace.setValue(BlockStateProperties.LIT, true)));
    Glow dark = loader.get(furnace.setValue(BlockStateProperties.LIT, false));
    assertEquals(new Glow(0, "black"), dark, "a string value redirects to another file");
    assertSame(dark, loader.get(furnace.setValue(BlockStateProperties.LIT, false).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)));
    assertEquals(new Glow(4, "none"), loader.get(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)), "empty key matches every state");
    assertNull(loader.get(Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.POWERED, true)), "a broken file adds none of its states");
    assertNull(loader.get(Blocks.STONE.defaultBlockState()), "empty object disables the block");
    Glow fallback = new Glow(8, "fallback");
    assertSame(fallback, loader.get(Blocks.DIRT.defaultBlockState(), fallback));
  }

  @Test
  void providersWriteFilesTheLoadersRead(@TempDir Path output) throws IOException {
    RegistryDataMapLoader<Block, Glow> glowLoader = new RegistryDataMapLoader<>("Test glow", PackType.SERVER_DATA, FOLDER, BuiltInRegistries.BLOCK, GLOW);
    new RegistryDataMapProvider<>(new PackOutput(output), Target.DATA_PACK, glowLoader, "mantle") {
      @Override
      protected void addEntries() {
        entry("shared", new Glow(7, "none"));
        entry(Blocks.STONE, new Glow(3, "red"));
        redirect(Blocks.DIRT, "shared");
      }

      @Override
      public String getName() {
        return "Test glow";
      }
    }.run(CachedOutput.NO_CACHE).join();

    BlockStateDataMapLoader<Glow> lightLoader = new BlockStateDataMapLoader<>("Test light", PackType.SERVER_DATA, "test/light", GLOW);
    new BlockStateDataMapProvider<>(new PackOutput(output), Target.DATA_PACK, lightLoader, "mantle") {
      @Override
      protected void addEntries() {
        entry("dark", new Glow(0, "black"));
        block(Blocks.FURNACE)
          .variant(new Glow(13, "none")).when(BlockStateProperties.LIT, true).end()
          .variant("dark").when(BlockStateProperties.LIT, false);
      }

      @Override
      public String getName() {
        return "Test light";
      }
    }.run(CachedOutput.NO_CACHE).join();

    Path data = output.resolve("data");
    assertEquals(json("{\"parent\": \"mantle:shared\"}"), json(Files.readString(data.resolve("minecraft/test/glow/dirt.json"))));
    glowLoader.apply(jsons(
      "minecraft:stone", Files.readString(data.resolve("minecraft/test/glow/stone.json")),
      "minecraft:dirt", Files.readString(data.resolve("minecraft/test/glow/dirt.json")),
      "mantle:shared", Files.readString(data.resolve("mantle/test/glow/shared.json"))
    ), ResourceManager.Empty.INSTANCE, TypedMap.EMPTY);
    assertEquals(new Glow(3, "red"), glowLoader.get(Blocks.STONE));
    assertEquals(new Glow(7, "none"), glowLoader.get(Blocks.DIRT));

    lightLoader.apply(jsons(
      "minecraft:furnace", Files.readString(data.resolve("minecraft/test/light/furnace.json")),
      "mantle:dark", Files.readString(data.resolve("mantle/test/light/dark.json"))
    ), ResourceManager.Empty.INSTANCE, TypedMap.EMPTY);
    BlockState furnace = Blocks.FURNACE.defaultBlockState();
    assertEquals(new Glow(13, "none"), lightLoader.get(furnace.setValue(BlockStateProperties.LIT, true)));
    assertEquals(new Glow(0, "black"), lightLoader.get(furnace.setValue(BlockStateProperties.LIT, false)));
  }
}
