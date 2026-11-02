package slimeknights.mantle.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * Helpers for block entities: safe lookups and ticker casting.
 * Not ported: upstream's deprecated typed get methods; use {@link #getLoaded} with instanceof.
 */
@SuppressWarnings("WeakerAccess")
public final class BlockEntityHelper {
  private BlockEntityHelper() {}

  /**
   * Checks if the position's chunk is loaded, so reading it will not load or generate chunks.
   * WHY deprecated hasChunk: 26.3 deprecates every LevelReader chunk check; Level has the replacement isLoaded,
   * but readers such as world generation regions have none, so they keep upstream's check.
   */
  @SuppressWarnings("deprecation")
  public static boolean isBlockLoaded(@Nullable BlockGetter world, BlockPos pos) {
    if (world == null) {
      return false;
    }
    if (world instanceof Level level) {
      return level.isLoaded(pos);
    }
    if (world instanceof LevelReader reader) {
      return reader.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }
    return true;
  }

  /** Gets the block entity if its chunk is loaded */
  @SuppressWarnings("unused")  // API
  public static @Nullable BlockEntity getLoaded(@Nullable BlockGetter world, BlockPos pos) {
    if (world != null && isBlockLoaded(world, pos)) {
      return world.getBlockEntity(pos);
    }
    return null;
  }

  /**
   * Returns the ticker when the block entity type is the expected one, otherwise null.
   * For {@link net.minecraft.world.level.block.EntityBlock#getTicker}, which receives any type.
   */
  @SuppressWarnings("unchecked")  // the types are equal, so the ticker fits
  public static <HAVE extends BlockEntity, RET extends BlockEntity> @Nullable BlockEntityTicker<RET> castTicker(
    BlockEntityType<RET> expected, BlockEntityType<HAVE> have, BlockEntityTicker<? super HAVE> ticker) {
    return have == expected ? (BlockEntityTicker<RET>) ticker : null;
  }

  /** Same as {@link #castTicker}, but only on the server */
  public static <HAVE extends BlockEntity, RET extends BlockEntity> @Nullable BlockEntityTicker<RET> serverTicker(
    Level level, BlockEntityType<RET> expected, BlockEntityType<HAVE> have, BlockEntityTicker<? super HAVE> ticker) {
    return level.isClientSide() ? null : castTicker(expected, have, ticker);
  }
}
