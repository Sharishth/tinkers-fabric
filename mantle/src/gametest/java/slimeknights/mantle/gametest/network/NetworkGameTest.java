package slimeknights.mantle.gametest.network;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import slimeknights.mantle.gametest.network.NetworkTestContent.Delivery;
import slimeknights.mantle.gametest.network.NetworkTestContent.PingPayload;
import slimeknights.mantle.gametest.network.NetworkTestContent.ProbePacket;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.util.JsonHelper;

import java.util.List;

import static slimeknights.mantle.gametest.network.NetworkTestContent.NETWORK;

/**
 * Checks Mantle networking that needs a running server.
 * Codecs are covered by unit tests; the real client round trip by tconstruct's NetworkClientGameTest.
 * blockEntityPacketChecksTheBlockEntity logs three expected "Failed to handle packet" errors.
 */
public class NetworkGameTest {
  /** Lectern position, relative to the test structure */
  private static final BlockPos LECTERN = new BlockPos(1, 1, 1);
  /** Distance well outside block interaction range */
  private static final double FAR_AWAY = 40;

  /** Places a lectern holding a book and a test player standing next to it */
  private static ServerPlayer playerNextToLectern(GameTestHelper helper) {
    helper.setBlock(LECTERN, Blocks.LECTERN);
    BlockPos lectern = helper.absolutePos(LECTERN);
    ServerPlayer player = TestPlayers.create(helper);
    player.setPos(lectern.getX() + 1.5, lectern.getY(), lectern.getZ() + 0.5);
    LecternBlock.tryPlaceBook(player, helper.getLevel(), lectern, helper.getLevel().getBlockState(lectern), new ItemStack(Items.WRITABLE_BOOK));
    helper.assertBlockProperty(LECTERN, LecternBlock.HAS_BOOK, true);
    return player;
  }

  /** Checks if the player carries a book and quill */
  private static boolean hasBook(ServerPlayer player) {
    return player.getInventory().contains(stack -> stack.is(Items.WRITABLE_BOOK));
  }

  @GameTest
  public void mantlePayloadsHaveServerHandlers(GameTestHelper helper) {
    helper.assertTrue(ServerPlayNetworking.getGlobalReceivers().contains(DropLecternBookPacket.TYPE.id()), "drop lectern book handler registered");
    helper.succeed();
  }

  @GameTest
  public void dropLecternBookGivesTheBookToThePlayer(GameTestHelper helper) {
    ServerPlayer player = playerNextToLectern(helper);
    new DropLecternBookPacket(helper.absolutePos(LECTERN)).handle(player);
    helper.assertTrue(hasBook(player), "book moved to the inventory");
    helper.assertBlockProperty(LECTERN, LecternBlock.HAS_BOOK, false);
    helper.assertFalse(helper.getBlockEntity(LECTERN, LecternBlockEntity.class).hasBook(), "lectern emptied");
    helper.succeed();
  }

  @GameTest
  public void dropLecternBookNeedsBuildRightsAndReach(GameTestHelper helper) {
    ServerPlayer player = playerNextToLectern(helper);
    BlockPos lectern = helper.absolutePos(LECTERN);
    DropLecternBookPacket packet = new DropLecternBookPacket(lectern);

    // adventure mode players cannot take books in vanilla either
    player.getAbilities().mayBuild = false;
    packet.handle(player);
    helper.assertBlockProperty(LECTERN, LecternBlock.HAS_BOOK, true);

    player.getAbilities().mayBuild = true;
    player.setPos(lectern.getX() + FAR_AWAY, lectern.getY(), lectern.getZ());
    packet.handle(player);
    helper.assertBlockProperty(LECTERN, LecternBlock.HAS_BOOK, true);
    helper.assertFalse(hasBook(player), "no book without reach or build rights");
    helper.succeed();
  }

  @GameTest
  public void dropLecternBookIgnoresOtherBlocks(GameTestHelper helper) {
    helper.setBlock(LECTERN, Blocks.STONE);
    ServerPlayer player = TestPlayers.create(helper);
    new DropLecternBookPacket(helper.absolutePos(LECTERN)).handle(player);
    helper.assertFalse(hasBook(player), "nothing to take");
    helper.succeed();
  }

  @GameTest
  public void syncPacketsSendsToNetworkPlayersInOrder(GameTestHelper helper) {
    // a test player has a channel-less connection, not an in-memory one, like a dedicated server client
    ServerPlayer player = TestPlayers.create(helper);
    helper.assertFalse(NetworkWrapper.isMemoryConnection(player), "test player is not on an in-memory connection");
    NETWORK.deliveries.clear();
    PingPayload first = new PingPayload("materials", 1);
    PingPayload second = new PingPayload("stats", 2);
    JsonHelper.syncPackets(player, NETWORK, first, second);
    helper.assertValueEqual(NETWORK.deliveries, List.of(new Delivery(player, first), new Delivery(player, second)), "deliveries");
    helper.succeed();
  }

  @GameTest
  public void sendToSkipsFakePlayersAndRejectsUnregisteredPayloads(GameTestHelper helper) {
    NETWORK.deliveries.clear();
    NETWORK.sendTo(new PingPayload("fake", 0), FakePlayer.get(helper.getLevel()));
    helper.assertTrue(NETWORK.deliveries.isEmpty(), "fake players get nothing");

    // DropLecternBookPacket is serverbound on Mantle's network, never clientbound on this one
    ServerPlayer player = TestPlayers.create(helper);
    try {
      NETWORK.sendTo(new DropLecternBookPacket(BlockPos.ZERO), player);
      helper.fail("sending an unregistered payload must throw");
    } catch (IllegalArgumentException e) {
      helper.assertTrue(e.getMessage().contains("mantle:drop_lectern_book"), "message names the payload: " + e.getMessage());
    }
    helper.assertTrue(NETWORK.deliveries.isEmpty(), "nothing delivered");
    helper.succeed();
  }

  @GameTest
  public void blockEntityPacketChecksTheBlockEntity(GameTestHelper helper) {
    helper.setBlock(LECTERN, Blocks.LECTERN);
    BlockPos lectern = helper.absolutePos(LECTERN);

    ProbePacket<LecternBlockEntity> match = ProbePacket.of(lectern, LecternBlockEntity.class);
    helper.assertTrue(match.handle(helper.getLevel()), "lectern handled");
    helper.assertValueEqual(match.handled(), List.of(helper.getBlockEntity(LECTERN, LecternBlockEntity.class)), "handled block entity");

    // expected log errors from here: wrong type, no block entity, no level
    ProbePacket<ChestBlockEntity> mismatch = ProbePacket.of(lectern, ChestBlockEntity.class);
    helper.assertFalse(mismatch.handle(helper.getLevel()), "type mismatch rejected");
    ProbePacket<LecternBlockEntity> empty = ProbePacket.of(lectern.above(), LecternBlockEntity.class);
    helper.assertFalse(empty.handle(helper.getLevel()), "missing block entity rejected");
    helper.assertFalse(match.handle(null), "missing level rejected");
    helper.assertValueEqual(mismatch.handled().size() + empty.handled().size(), 0, "nothing else handled");
    helper.succeed();
  }
}
