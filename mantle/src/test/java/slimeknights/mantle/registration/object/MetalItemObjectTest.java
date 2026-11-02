package slimeknights.mantle.registration.object;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.registration.RegistrationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Checks MetalItemObject and RegistrationHelper with vanilla iron, which needs no registration. */
class MetalItemObjectTest {
  private static MetalItemObject iron;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    iron = new MetalItemObject("iron", new ItemObject<>(BuiltInRegistries.BLOCK, Blocks.IRON_BLOCK), () -> Items.IRON_INGOT, () -> Items.IRON_NUGGET);
  }

  @Test
  void valuesListBlockIngotAndNugget() {
    // upstream bug: values() returned the ingot twice and no nugget
    assertEquals(List.of(Blocks.IRON_BLOCK, Items.IRON_INGOT, Items.IRON_NUGGET), iron.values());
  }

  @Test
  void forEachMatchesValues() {
    List<ItemLike> seen = new ArrayList<>();
    iron.forEach(seen::add);
    assertEquals(iron.values(), seen);
  }

  @Test
  void tagsUseCommonNamespace() {
    assertEquals(Identifier.fromNamespaceAndPath("c", "storage_blocks/iron"), iron.getBlockTag().location());
    assertEquals(Identifier.fromNamespaceAndPath("c", "storage_blocks/iron"), iron.getBlockItemTag().location());
    assertEquals(Identifier.fromNamespaceAndPath("c", "ingots/iron"), iron.getIngotTag().location());
    assertEquals(Identifier.fromNamespaceAndPath("c", "nuggets/iron"), iron.getNuggetTag().location());
  }

  @Test
  void castedHolderReturnsRegisteredEntry() {
    Supplier<Block> holder = RegistrationHelper.getCastedHolder(BuiltInRegistries.BLOCK, Blocks.STONE);
    assertSame(Blocks.STONE, holder.get());
  }
}
