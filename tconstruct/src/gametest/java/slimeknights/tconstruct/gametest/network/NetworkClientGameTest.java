package slimeknights.tconstruct.gametest.network;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.gametest.network.NetworkTestContent.LecternPingPacket;

import static slimeknights.tconstruct.gametest.network.NetworkTestContent.NETWORK;
import static slimeknights.tconstruct.gametest.network.NetworkTestContent.RECEIVED;

/**
 * Sends Mantle payloads both ways between a real client and its integrated server.
 * Covers what server GameTests cannot: payload types known on both sides, the client receiver registered from a
 * client entrypoint, and the in-memory connection that syncPackets skips.
 */
public class NetworkClientGameTest implements FabricClientGameTest {
  /** Ticks a payload that should not arrive gets to arrive anyway */
  private static final int SETTLE_TICKS = 20;
  /** Lectern offset from the player, inside block interaction range */
  private static final int LECTERN_OFFSET = 2;

  @Override
  public void runTest(ClientGameTestContext context) {
    try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
      singleplayer.getConnection().waitForChunksRender();
      TestServerContext server = singleplayer.getServer();
      BlockPos lectern = server.computeOnServer(NetworkClientGameTest::placeLecternWithBook);
      // WHY: payloads can overtake the block update; the client block entity must exist before pinging it
      context.waitFor(client -> client.level != null && client.level.getBlockState(lectern).is(Blocks.LECTERN));

      checkSyncSkipsTheHost(context, server, lectern);
      checkClientboundReachesBlockEntity(context, server, lectern);
      checkServerboundTakesBook(context, server, lectern);
    }
  }

  /** The host shares the server's data, so datapack sync payloads are not sent to it */
  private static void checkSyncSkipsTheHost(ClientGameTestContext context, TestServerContext server, BlockPos lectern) {
    RECEIVED.set(null);
    boolean memory = server.computeOnServer(s -> NetworkWrapper.isMemoryConnection(onlyPlayer(s)));
    require(memory, "the single player host should be on an in-memory connection");
    server.runOnServer(s -> JsonHelper.syncPackets(onlyPlayer(s), NETWORK, new LecternPingPacket(lectern, "sync")));
    context.waitTicks(SETTLE_TICKS);
    require(RECEIVED.get() == null, "syncPackets sent to the host: " + RECEIVED.get());
  }

  /** A clientbound block entity payload finds the client's lectern */
  private static void checkClientboundReachesBlockEntity(ClientGameTestContext context, TestServerContext server, BlockPos lectern) {
    server.runOnServer(s -> NETWORK.sendToClientsAround(new LecternPingPacket(lectern, "around"), s.overworld(), lectern));
    context.waitFor(client -> "around".equals(RECEIVED.get()));
  }

  /** A serverbound payload from the client takes the book off the lectern */
  private static void checkServerboundTakesBook(ClientGameTestContext context, TestServerContext server, BlockPos lectern) {
    context.runOnClient(client -> ClientPlayNetworking.send(new DropLecternBookPacket(lectern)));
    server.waitFor(s -> onlyPlayer(s).getInventory().contains(stack -> stack.is(Items.WRITABLE_BOOK)));
    boolean hasBook = server.computeOnServer(s -> s.overworld().getBlockState(lectern).getValue(LecternBlock.HAS_BOOK));
    require(!hasBook, "the lectern still holds its book");
  }

  /** Places a lectern with a book near the player and returns its position */
  private static BlockPos placeLecternWithBook(MinecraftServer server) {
    ServerPlayer player = onlyPlayer(server);
    ServerLevel level = player.level();
    BlockPos pos = player.blockPosition().east(LECTERN_OFFSET);
    level.setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState());
    LecternBlock.tryPlaceBook(player, level, pos, level.getBlockState(pos), new ItemStack(Items.WRITABLE_BOOK));
    return pos;
  }

  /** Gets the single player */
  private static ServerPlayer onlyPlayer(MinecraftServer server) {
    return server.getPlayerList().getPlayers().getFirst();
  }

  /** Fails the test with a message when the condition is false */
  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
