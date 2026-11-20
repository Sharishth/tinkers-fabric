package slimeknights.mantle.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import slimeknights.mantle.network.packet.BlockEntityPacket;

/**
 * Client receivers for block entity payloads.
 * Replaces the client half of upstream's BlockEntityPacket.handleThreadsafe, which read the level through
 * SafeClientAccess; on Fabric the receiver lives in client code and gets the level from its context.
 */
public final class BlockEntityPacketHandler {
  private BlockEntityPacketHandler() {}

  /**
   * Registers the client receiver for a block entity payload; call from a client entrypoint.
   * The payload type must also be registered as clientbound on its NetworkWrapper from the common entrypoint.
   * Fabric runs the receiver on the client thread.
   * @param type  Payload type
   * @param <P>  Payload class
   */
  public static <P extends BlockEntityPacket<?>> void register(CustomPacketPayload.Type<P> type) {
    boolean registered = ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> payload.handle(context.client().level));
    if (!registered) {
      throw new IllegalStateException("Payload " + type.id() + " already has a client receiver; register it only once");
    }
  }
}
