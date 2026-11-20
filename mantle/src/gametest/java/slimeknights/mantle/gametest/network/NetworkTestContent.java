package slimeknights.mantle.gametest.network;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.BlockEntityPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Payloads and a recording network used by {@link NetworkGameTest}.
 * Registered at mod init like real payloads, since Fabric's payload registry is global.
 */
public class NetworkTestContent implements ModInitializer {
  /** Id of this test mod, from its fabric.mod.json */
  private static final String MOD_ID = "mantle-gametest";

  /** Clientbound payload whose codec is a Mantle loadable */
  public record PingPayload(String message, int count) implements CustomPacketPayload {
    public static final Type<PingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "ping"));
    public static final RecordLoadable<PingPayload> LOADABLE = RecordLoadable.create(
      StringLoadable.DEFAULT.requiredField("message", PingPayload::message),
      IntLoadable.ANY_FULL.requiredField("count", PingPayload::count),
      PingPayload::new);

    @Override
    public Type<PingPayload> type() {
      return TYPE;
    }
  }

  /** One payload handed to Fabric for one player */
  public record Delivery(ServerPlayer player, CustomPacketPayload payload) {}

  /** Network that records deliveries instead of sending them, so tests can check targets */
  public static class RecordingNetwork extends NetworkWrapper {
    /** Deliveries in order; tests clear it first, GameTest functions run one at a time on the server thread */
    public final List<Delivery> deliveries = new ArrayList<>();

    RecordingNetwork() {
      super(MOD_ID);
    }

    @Override
    protected void deliver(ServerPlayer player, CustomPacketPayload payload) {
      deliveries.add(new Delivery(player, payload));
    }
  }

  /**
   * Block entity payload that remembers what it handled; never sent, only dispatched directly.
   * @param <T> Expected block entity class
   */
  public record ProbePacket<T>(BlockPos pos, Class<T> blockEntityClass, List<T> handled) implements BlockEntityPacket<T> {
    public static final Type<ProbePacket<?>> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "probe"));

    /** Creates a probe with an empty handled list */
    public static <T> ProbePacket<T> of(BlockPos pos, Class<T> blockEntityClass) {
      return new ProbePacket<>(pos, blockEntityClass, new ArrayList<>());
    }

    @Override
    public void handleBlockEntity(T be) {
      handled.add(be);
    }

    @Override
    public Type<ProbePacket<?>> type() {
      return TYPE;
    }
  }

  public static final RecordingNetwork NETWORK = new RecordingNetwork();

  @Override
  public void onInitialize() {
    NETWORK.registerClientbound(PingPayload.TYPE, PingPayload.LOADABLE);
  }
}
