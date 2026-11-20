package slimeknights.mantle.gametest.network;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Fresh, real (not fake) server players for GameTests.
 * WHY: 26.3 deprecates GameTestHelper's mock player helpers for removal. The connection is built like Fabric's
 * FakePlayer builds its own: a Connection without a channel, which is not in-memory, so it stands in for a remote client.
 */
final class TestPlayers {
  /** Name for every test player; the UUID keeps them apart */
  private static final String NAME = "mantle_test";

  private TestPlayers() {}

  /** Creates a player in the test level whose packets go nowhere */
  static ServerPlayer create(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    GameProfile profile = new GameProfile(UUID.randomUUID(), NAME);
    ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
    player.connection = new SilentPacketListener(player);
    return player;
  }

  /** Play listener that drops every outgoing packet */
  private static final class SilentPacketListener extends ServerGamePacketListenerImpl {
    SilentPacketListener(ServerPlayer player) {
      super(player.level().getServer(), new Connection(PacketFlow.CLIENTBOUND), player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    @Override
    public void send(Packet<?> packet) {}

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {}
  }
}
