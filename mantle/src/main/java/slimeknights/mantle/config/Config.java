package slimeknights.mantle.config;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.Mantle;

import java.util.List;

/**
 * Mantle's own config options.
 * Built on Forge Config API Port's copy of NeoForge's {@link ModConfigSpec} (D-07): same TOML files, comments and
 * server-to-client sync as upstream's ForgeConfigSpec.
 * A value can only be read once its config is loaded: client options after the client entrypoint, server options
 * while a world is running. Reading earlier throws {@link IllegalStateException}.
 */
@Internal
public final class Config {
  private Config() {}

  /** How Mantle draws the health bar */
  public enum HeartRenderer {
    DISABLE, NO_MAX, WITH_MAX
  }

  /** Heart renderer mode */
  public static final EnumValue<HeartRenderer> HEART_RENDERER;

  /** If true, enables the fluid fog fix. If false, disables it for better shader compatibility. */
  public static final BooleanValue ENABLE_FLUID_FOG_FIX;

  /** If true, the fallback shader for fluids is a text shader, which provides better compatibility. */
  public static final BooleanValue FLUID_USE_TEXT_SHADER;

  /** Mod ids to prefer, in order, when a tag has entries from several mods */
  private static final List<String> DEFAULT_TAG_PREFERENCES = List.of("minecraft", "tconstruct", "tmechworks", "metalborn", "embers", "create", "immersiveengineering", "mekanism", "thermal");
  /** Namespaces preferred for recipe outputs chosen from tags */
  public static final ConfigValue<List<? extends String>> TAG_PREFERENCES;

  /** Options stored per game install, read on the client only */
  public static final ModConfigSpec CLIENT_SPEC;
  /** Options stored on the server and synced to players */
  public static final ModConfigSpec SERVER_SPEC;

  static {
    ModConfigSpec.Builder client = new ModConfigSpec.Builder();
    ModConfigSpec.Builder server = new ModConfigSpec.Builder();

    // TODO(T-72): tell mod authors how to turn the heart renderer off, once its render hook exists
    HEART_RENDERER = client
      .comment(
        "If not DISABLE, enables the Mantle heart renderer, which stacks hearts by changing the color instead of vertically stacking them.",
        "WITH_MAX will show the max health in colored containers behind the health bar. NO_MAX will show just the health bar",
        "If DISABLE, uses the vanilla heart renderer.")
      .translation("config.mantle.extraHeartRenderer")
      .defineEnum("heartRenderer", HeartRenderer.WITH_MAX);

    ENABLE_FLUID_FOG_FIX = client
      .comment(
        "If true, fluids properly have their lighting adjusted under vanilla fog effects such as blindness. If false, they render as nearly fullbright ignoring fog and have limited light level support.",
        "This config option is provided as the fix breaks shaders, and slightly broken is better than fully broken.",
        "Best fix is to fix your shaders though, so you can have no broken visuals.")
      .translation("config.mantle.enableFluidFogFix")
      .define("enableFluidFogFix", true);

    FLUID_USE_TEXT_SHADER = client
      .comment(
        "If true, the fallback shader for fluid uses a text shader, which provides better compatibility. If false, uses the generic position color tex lightmap shader.",
        "The text shader provides a fallback with more functionality than the generic one, but may be unexpected by other custom rendering.",
        "Does nothing if enableFluidFogFix is true.")
      .translation("config.mantle.fluidFallbackUseTextShader")
      .define("fluidFallbackUseTextShader", true);

    TAG_PREFERENCES = server
      .comment("Preferences for outputs from tags used in automatic compat in recipes")
      .translation("config.mantle.tagPreferences")
      .defineList("tagPreferences", DEFAULT_TAG_PREFERENCES, () -> "", element -> element instanceof String);

    CLIENT_SPEC = client.build();
    SERVER_SPEC = server.build();
  }

  /**
   * Registers the server config; call from the main entrypoint.
   * The file loads when a world starts and is synced to players who join.
   */
  public static void registerServer() {
    ConfigRegistry.INSTANCE.register(Mantle.modId, ModConfig.Type.SERVER, SERVER_SPEC);
  }

  /**
   * Registers the client config; call from the client entrypoint.
   * WHY not in the main entrypoint like upstream: client and common configs load the moment they are registered,
   * so registering it there would create a client config file on dedicated servers too.
   */
  public static void registerClient() {
    ConfigRegistry.INSTANCE.register(Mantle.modId, ModConfig.Type.CLIENT, CLIENT_SPEC);
  }
}
