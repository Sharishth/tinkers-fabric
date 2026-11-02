package slimeknights.mantle.gametest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import slimeknights.mantle.Mantle;

/** Server smoke tests: Mantle loads into a world. */
public class MantleSmokeGameTest {
  @GameTest
  public void mantleIsLoaded(GameTestHelper helper) {
    helper.assertTrue(FabricLoader.getInstance().isModLoaded("mantle"), "mantle should be loaded");
    helper.succeed();
  }

  @GameTest
  public void mantleEntrypointRanOnce(GameTestHelper helper) {
    helper.assertTrue(Mantle.instance != null, "Mantle.onInitialize should have set the instance");
    // WHY: guards against a second Mantle instance, e.g. listed twice in fabric.mod.json
    long mantleEntrypoints = FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream()
      .filter(Mantle.class::isInstance)
      .count();
    helper.assertTrue(mantleEntrypoints == 1, "expected one Mantle main entrypoint, found " + mantleEntrypoints);
    helper.assertTrue(FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).contains(Mantle.instance),
      "Mantle.instance should be the entrypoint Fabric created");
    helper.succeed();
  }
}
