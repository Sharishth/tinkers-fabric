package slimeknights.mantle.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Door block with a public constructor.
 * WHY: 26.x made DoorBlock's constructor protected; upstream constructed vanilla doors directly.
 */
public class MantleDoorBlock extends DoorBlock {
  public MantleDoorBlock(BlockSetType type, BlockBehaviour.Properties properties) {
    super(type, properties);
  }
}
