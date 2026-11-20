package slimeknights.mantle.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.config.Config.HeartRenderer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the config specs without loading a file; loading from disk is covered by ConfigGameTest */
class ConfigTest {
  private static final String TAG_PREFERENCES = "tagPreferences";

  @Test
  void specsKeepUpstreamKeysAndDefaults() {
    assertEquals(List.of("heartRenderer"), Config.HEART_RENDERER.getPath());
    assertEquals(HeartRenderer.WITH_MAX, Config.HEART_RENDERER.getDefault());
    assertEquals(List.of("enableFluidFogFix"), Config.ENABLE_FLUID_FOG_FIX.getPath());
    assertTrue(Config.ENABLE_FLUID_FOG_FIX.getDefault());
    assertEquals(List.of("fluidFallbackUseTextShader"), Config.FLUID_USE_TEXT_SHADER.getPath());
    assertTrue(Config.FLUID_USE_TEXT_SHADER.getDefault());
    assertEquals(List.of(TAG_PREFERENCES), Config.TAG_PREFERENCES.getPath());
    List<? extends String> preferences = Config.TAG_PREFERENCES.getDefault();
    assertEquals(9, preferences.size());
    assertEquals("minecraft", preferences.get(0), "vanilla ids are preferred first");
    assertEquals("tconstruct", preferences.get(1));
  }

  @Test
  void clientAndServerOptionsLiveInSeparateSpecs() {
    assertTrue(Config.CLIENT_SPEC.getSpec().contains("heartRenderer"));
    assertFalse(Config.CLIENT_SPEC.getSpec().contains(TAG_PREFERENCES));
    assertTrue(Config.SERVER_SPEC.getSpec().contains(TAG_PREFERENCES));
    assertFalse(Config.SERVER_SPEC.getSpec().contains("heartRenderer"));
  }

  @Test
  void valuesCannotBeReadBeforeTheirConfigLoads() {
    // unit tests run no entrypoint and no world, so neither config is ever loaded here
    assertFalse(Config.SERVER_SPEC.isLoaded());
    assertThrows(IllegalStateException.class, Config.TAG_PREFERENCES::get);
    assertThrows(IllegalStateException.class, Config.HEART_RENDERER::get);
  }

  @Test
  void serverSpecAcceptsListsAndRepairsBrokenFiles() {
    // correct() is what runs on a loaded file: it repairs values and restores comments
    CommentedConfig custom = CommentedConfig.inMemory();
    custom.set(TAG_PREFERENCES, List.of("create", "minecraft"));
    Config.SERVER_SPEC.correct(custom);
    assertEquals(List.of("create", "minecraft"), custom.get(TAG_PREFERENCES), "a pack author's own order is kept");
    assertTrue(Config.SERVER_SPEC.isCorrect(custom), "nothing left to repair after correcting");

    CommentedConfig broken = CommentedConfig.inMemory();
    broken.set(TAG_PREFERENCES, "minecraft");
    assertFalse(Config.SERVER_SPEC.isCorrect(broken), "a plain string is not a list");
    Config.SERVER_SPEC.correct(broken);
    assertEquals(Config.TAG_PREFERENCES.getDefault(), broken.get(TAG_PREFERENCES), "broken value replaced by the default");

    CommentedConfig empty = CommentedConfig.inMemory();
    Config.SERVER_SPEC.correct(empty);
    assertEquals(Config.TAG_PREFERENCES.getDefault(), empty.get(TAG_PREFERENCES), "missing value filled in");
  }
}
