package slimeknights.mantle.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.BlockEntityHelper;

/**
 * Clientbound payload that updates one block entity.
 * Client code registers the receiver with Mantle's client BlockEntityPacketHandler, which calls
 * {@link #handle(BlockGetter)} with the client level. Upstream passed the Forge network context to
 * {@link #handleBlockEntity}; no Tinkers packet read it, and Fabric's client context is client-only code.
 * @param <T> Block entity class, or an interface block entities implement
 */
public interface BlockEntityPacket<T> extends CustomPacketPayload {
  /** Gets the position of the block entity to update */
  BlockPos pos();

  /**
   * Gets the class the block entity must have.
   * Upstream named this type(), which now clashes with CustomPacketPayload.type().
   */
  Class<T> blockEntityClass();

  /**
   * Applies the update to the block entity.
   * @param be  Loaded block entity of the expected type
   */
  void handleBlockEntity(T be);

  /**
   * Finds the block entity and applies the update; logs why when it cannot.
   * @param level  Level to look in, normally the client level
   * @return  True if the update was applied
   */
  default boolean handle(@Nullable BlockGetter level) {
    BlockPos pos = pos();
    if (level == null || !BlockEntityHelper.isBlockLoaded(level, pos)) {
      Mantle.logger.error("Failed to handle packet {}: World is not loaded at {}", this, pos);
      return false;
    }
    // upstream logged an unloaded position twice; each failure now logs one line
    BlockEntity be = level.getBlockEntity(pos);
    if (be == null) {
      Mantle.logger.error("Failed to handle packet {}: No block entity at {}", this, pos);
      return false;
    }
    Class<T> type = blockEntityClass();
    if (!type.isInstance(be)) {
      Mantle.logger.error("Failed to handle packet {}: Block entity type mismatch at {}, expected {}, found {}", this, pos, type, be.getClass());
      return false;
    }
    handleBlockEntity(type.cast(be));
    return true;
  }

  /**
   * Gets a block entity for a packet, checking the position is loaded first.
   * @param level   Level instance
   * @param pos     Position
   * @param packet  Object to print in the log
   * @return  Block entity, or null if the level is null, the position is not loaded, or no block entity is there
   */
  static @Nullable BlockEntity getBlockEntity(@Nullable BlockGetter level, BlockPos pos, Object packet) {
    if (level != null && BlockEntityHelper.isBlockLoaded(level, pos)) {
      return level.getBlockEntity(pos);
    }
    Mantle.logger.error("Failed to handle packet {}: World is not loaded at {}", packet, pos);
    return null;
  }
}
