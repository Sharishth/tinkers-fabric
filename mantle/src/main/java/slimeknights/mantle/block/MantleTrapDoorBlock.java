package slimeknights.mantle.block;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Trapdoor block with a public constructor.
 * WHY: 26.x made TrapDoorBlock's constructor protected; upstream constructed vanilla trapdoors directly.
 */
public class MantleTrapDoorBlock extends TrapDoorBlock {
  public MantleTrapDoorBlock(BlockSetType type, BlockBehaviour.Properties properties) {
    super(type, properties);
  }
}
