package slimeknights.tconstruct.gametest.network;

import net.fabricmc.api.ClientModInitializer;
import slimeknights.mantle.client.network.BlockEntityPacketHandler;
import slimeknights.tconstruct.gametest.network.NetworkTestContent.LecternPingPacket;

/** Registers the client receiver for the test payload, from the client entrypoint as real mods do */
public class NetworkTestClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    BlockEntityPacketHandler.register(LecternPingPacket.TYPE);
  }
}
