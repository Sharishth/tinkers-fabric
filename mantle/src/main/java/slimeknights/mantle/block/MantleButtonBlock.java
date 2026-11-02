package slimeknights.mantle.block;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Button block with a public constructor.
 * WHY: 26.x made ButtonBlock's constructor protected; upstream constructed vanilla buttons directly.
 * Whether arrows press the button is now part of the {@link BlockSetType}.
 */
public class MantleButtonBlock extends ButtonBlock {
  /**
   * Creates a button.
   * @param type                Block set, gives the sounds and whether arrows press it
   * @param ticksToStayPressed  Ticks the button stays pressed, 30 for wood and 20 for stone
   * @param properties          Block properties
   */
  public MantleButtonBlock(BlockSetType type, int ticksToStayPressed, BlockBehaviour.Properties properties) {
    super(type, ticksToStayPressed, properties);
  }
}
