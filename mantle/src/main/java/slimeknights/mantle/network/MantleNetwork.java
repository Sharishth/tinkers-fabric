package slimeknights.mantle.network;

import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.DropLecternBookPacket;

/**
 * Mantle's own payloads.
 * Upstream's SwingArmPacket is gone: 26.3 vanilla LivingEntity.swing(hand, animation, updateSelf) broadcasts
 * ClientboundSwingAnimationPacket without resetting the attack cooldown, which is all the packet did (see PORTING.md).
 */
public class MantleNetwork {
  /** Network for payloads in the mantle namespace */
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.modId);

  private MantleNetwork() {}

  /** Registers Mantle's payloads; called once from the common entrypoint */
  public static void registerPackets() {
    INSTANCE.registerServerbound(DropLecternBookPacket.TYPE, DropLecternBookPacket.STREAM_CODEC);
    // TODO(T-73): book payloads OpenLecternBook, OpenNamedBook (clientbound) and UpdateHeldPage, UpdateInventoryPage, UpdateLecternPage (serverbound); they need BookHelper's page data component, ILecternBookItem and the book screen
    // TODO(T-50): FluidContainerTransferPacket (clientbound, datapack sync through JsonHelper.syncPackets)
  }
}
