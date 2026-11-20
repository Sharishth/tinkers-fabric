package slimeknights.mantle.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.Mantle;

/**
 * Asks the server to move the book on a lectern into the sender's inventory.
 * Sent by Mantle's lectern book screen (T-73).
 * @param pos  Lectern position
 */
public record DropLecternBookPacket(BlockPos pos) implements IServerboundPacket {
  public static final Type<DropLecternBookPacket> TYPE = new Type<>(Mantle.getResource("drop_lectern_book"));
  public static final StreamCodec<ByteBuf, DropLecternBookPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(DropLecternBookPacket::new, DropLecternBookPacket::pos);

  @Override
  public Type<DropLecternBookPacket> type() {
    return TYPE;
  }

  @Override
  public void handle(ServerPlayer sender) {
    ServerLevel level = sender.level();
    // WHY: vanilla's take book button needs build rights and reach; upstream checked neither, so any client
    // could pull books from any loaded lectern
    if (!sender.mayBuild() || !level.isLoaded(pos)) {
      return;
    }
    BlockState state = level.getBlockState(pos);
    if (!(state.getBlock() instanceof LecternBlock) || !state.getValue(LecternBlock.HAS_BOOK)) {
      return;
    }
    if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) || !Container.stillValidBlockEntity(lectern, sender)) {
      return;
    }
    ItemStack book = lectern.getBook().copy();
    if (book.isEmpty()) {
      return;
    }
    lectern.clearContent();
    // vanilla's own book removal: unpowers, clears HAS_BOOK, updates the block below and emits the game event
    LecternBlock.resetBookState(sender, level, pos, state, false);
    // same give-or-drop as vanilla's lectern menu
    if (!sender.getInventory().add(book)) {
      sender.drop(book, false, Prediction.PREDICTED);
    }
  }
}
