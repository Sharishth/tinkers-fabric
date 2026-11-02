package slimeknights.mantle.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

/** Server smoke tests: Mantle loads into a world. */
public class MantleSmokeGameTest {
  @GameTest
  public void mantleIsLoaded(GameTestHelper helper) {
    helper.assertTrue(FabricLoader.getInstance().isModLoaded("mantle"), "mantle should be loaded");
    helper.succeed();
  }
}
