package slimeknights.tconstruct.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

/** Server smoke tests: both mods load into a world. */
public class TConstructSmokeGameTest {
  @GameTest
  public void tconstructAndMantleAreLoaded(GameTestHelper helper) {
    FabricLoader loader = FabricLoader.getInstance();
    helper.assertTrue(loader.isModLoaded("tconstruct"), "tconstruct should be loaded");
    helper.assertTrue(loader.isModLoaded("mantle"), "mantle should be loaded (jar-in-jar dependency)");
    helper.succeed();
  }
}
