package slimeknights.mantle.data.predicate.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;

/**
 * Predicate matching the block at the entity's position, optionally shifted up or down.
 * @param block   Block predicate
 * @param offset  Blocks above (positive) or below (negative) the entity's block
 */
public record BlockAtEntityPredicate(IJsonPredicate<BlockState> block, int offset) implements LivingEntityPredicate {
  public static final RecordLoadable<BlockAtEntityPredicate> LOADER = RecordLoadable.create(
    BlockPredicate.LOADER.directField("block_type", BlockAtEntityPredicate::block),
    IntLoadable.ANY_BYTE.defaultField("offset", 0, false, BlockAtEntityPredicate::offset),
    BlockAtEntityPredicate::new);

  @Override
  public boolean matches(LivingEntity entity) {
    BlockPos pos = entity.blockPosition();
    if (offset != 0) {
      pos = pos.relative(Direction.Axis.Y, offset);
    }
    return block.matches(entity.level().getBlockState(pos));
  }

  @Override
  public RecordLoadable<BlockAtEntityPredicate> getLoader() {
    return LOADER;
  }
}
