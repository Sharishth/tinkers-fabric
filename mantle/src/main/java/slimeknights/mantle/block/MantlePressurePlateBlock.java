package slimeknights.mantle.block;

import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Pressure plate block with a public constructor.
 * WHY: 26.x made PressurePlateBlock's constructor protected; upstream constructed vanilla plates directly.
 * The sensitivity that upstream passed in is now part of the {@link BlockSetType}.
 */
public class MantlePressurePlateBlock extends PressurePlateBlock {
  public MantlePressurePlateBlock(BlockSetType type, BlockBehaviour.Properties properties) {
    super(type, properties);
  }
}
