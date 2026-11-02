package slimeknights.mantle.test;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves unit tests can bootstrap Minecraft registries. */
class MinecraftBootstrapTest {
  @BeforeAll
  static void bootstrap() {
    // WHY: fabric-loader-junit only sets up the loader; vanilla registries still need an explicit bootstrap
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void vanillaItemRegistryIsPopulated() {
    assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace("stone")));
  }
}
