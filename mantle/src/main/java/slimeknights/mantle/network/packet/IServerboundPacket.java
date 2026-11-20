package slimeknights.mantle.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Payload a client sends to the server that handles itself.
 * Replaces upstream's ISimplePacket and IThreadsafePacket for serverbound packets: encoding moves to the payload's
 * StreamCodec, and Fabric already runs serverbound handlers on the server thread, so no work queue is needed.
 * Register with {@link slimeknights.mantle.network.NetworkWrapper#registerServerbound(CustomPacketPayload.Type, net.minecraft.network.codec.StreamCodec)}.
 */
public interface IServerboundPacket extends CustomPacketPayload {
  /**
   * Handles the payload on the server thread.
   * @param sender  Player whose client sent the payload
   */
  void handle(ServerPlayer sender);
}
