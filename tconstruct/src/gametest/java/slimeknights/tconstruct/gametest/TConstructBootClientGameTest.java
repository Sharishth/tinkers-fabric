package slimeknights.tconstruct.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Client smoke test: boots to title, then opens a world.
 * Screenshots land in the client game test run directory for a person to inspect.
 */
public class TConstructBootClientGameTest implements FabricClientGameTest {
  /** Ticks for the title menu fade-in to finish (about two seconds). */
  private static final int TITLE_FADE_IN_TICKS = 40;

  @Override
  public void runTest(ClientGameTestContext context) {
    context.waitForScreen(TitleScreen.class);
    // WHY: the title menu fades in; without a wait the screenshot shows a half-transparent menu
    context.waitTicks(TITLE_FADE_IN_TICKS);
    context.takeScreenshot("tconstruct-title-screen");

    try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
      // WHY: a fixed tick wait can screenshot half-loaded terrain; wait for real chunk rendering
      singleplayer.getConnection().waitForChunksRender();
      context.takeScreenshot("tconstruct-singleplayer-world");
    }
  }
}
