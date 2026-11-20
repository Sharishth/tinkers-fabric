package slimeknights.mantle.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.config.Config;

import java.nio.file.Files;
import java.util.List;

/** Checks Mantle's server config is registered, loaded from disk and readable while a world runs */
public class ConfigGameTest {
  @GameTest
  public void serverConfigLoadsWhenTheWorldStarts(GameTestHelper helper) {
    helper.assertTrue(Config.SERVER_SPEC.isLoaded(), "server config is loaded while the server runs");
    List<? extends String> preferences = Config.TAG_PREFERENCES.get();
    helper.assertValueEqual(preferences, Config.TAG_PREFERENCES.getDefault(), "a fresh config file holds the defaults");

    List<ModConfig> serverConfigs = ModConfigs.getModConfigs(Mantle.modId).stream()
      .filter(config -> config.getType() == ModConfig.Type.SERVER)
      .toList();
    helper.assertValueEqual(serverConfigs.size(), 1, "one server config registered for mantle");
    ModConfig config = serverConfigs.getFirst();
    helper.assertValueEqual(config.getFileName(), "mantle-server.toml", "config file name");
    helper.assertTrue(Files.isRegularFile(config.getFullPath()), "config file written at " + config.getFullPath());
    helper.succeed();
  }
}
