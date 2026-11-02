package slimeknights.mantle.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stair block with a public constructor.
 * WHY: 26.x made StairBlock's constructor protected; upstream used Forge's public supplier constructor.
 */
public class MantleStairBlock extends StairBlock {
  public MantleStairBlock(BlockState baseState, BlockBehaviour.Properties properties) {
    super(baseState, properties);
  }

  /** Gets the block state these stairs copy their behaviour from */
  public BlockState getBaseState() {
    return baseState;
  }
}
