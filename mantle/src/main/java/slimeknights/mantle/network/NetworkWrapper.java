package slimeknights.mantle.network;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.IServerboundPacket;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers one mod's payloads with Fabric and sends them.
 * Ports upstream's SimpleChannel wrapper onto vanilla CustomPacketPayload, StreamCodec and Fabric's
 * PayloadTypeRegistry (D-15); the payload approach follows justduck25's Continuum-Mantle 26.1 port.
 * Register every payload from the common entrypoint, which runs on clients and servers alike, so both sides know
 * every type before anything is sent. Clientbound handlers are client code: register them from the client entrypoint
 * with ClientPlayNetworking (or Mantle's client BlockEntityPacketHandler); client code sends with ClientPlayNetworking.send.
 * Upstream's channel version string is gone: Fabric has no per-channel versions (see PORTING.md).
 */
public class NetworkWrapper {
  /** Namespace every payload id of this network must use */
  private final String modId;
  /** Ids registered as clientbound, checked before sending so a mistake fails at the call site */
  private final Set<Identifier> clientbound = ConcurrentHashMap.newKeySet();

  /**
   * Creates a network for one mod.
   * @param modId  Namespace of every payload id registered here
   */
  public NetworkWrapper(String modId) {
    this.modId = modId;
  }


  /* Registration */

  /**
   * Registers a payload the server sends to clients.
   * Loadables are StreamCodecs on RegistryFriendlyByteBuf (D-20), so a RecordLoadable can be passed as the codec.
   * @param type   Payload type, id in this network's namespace
   * @param codec  Payload codec
   * @param <T>  Payload class
   */
  public <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    checkNamespace(type);
    StreamCodec<RegistryFriendlyByteBuf, T> logged = logErrors(type, codec);
    PayloadTypeRegistry.clientboundPlay().register(type, logged);
    clientbound.add(type.id());
  }

  /**
   * Registers a payload clients send to the server, handled by the payload itself.
   * @param type   Payload type, id in this network's namespace
   * @param codec  Payload codec
   * @param <T>  Payload class
   */
  public <T extends IServerboundPacket> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    registerServerbound(type, codec, (payload, context) -> payload.handle(context.player()));
  }

  /**
   * Registers a payload clients send to the server with a separate handler.
   * Fabric runs the handler on the server thread.
   * @param type     Payload type, id in this network's namespace
   * @param codec    Payload codec
   * @param handler  Server side handler
   * @param <T>  Payload class
   */
  public <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
    checkNamespace(type);
    StreamCodec<RegistryFriendlyByteBuf, T> logged = logErrors(type, codec);
    PayloadTypeRegistry.serverboundPlay().register(type, logged);
    if (!ServerPlayNetworking.registerGlobalReceiver(type, handler)) {
      throw new IllegalStateException("Payload " + type.id() + " already has a server handler; register it only once");
    }
  }

  /** Rejects payload ids outside this network's namespace */
  private void checkNamespace(CustomPacketPayload.Type<?> type) {
    if (!modId.equals(type.id().getNamespace())) {
      throw new IllegalArgumentException("Payload " + type.id() + " does not belong to network '" + modId + "'; use an id in that namespace");
    }
  }

  /**
   * Wraps a codec so a failure logs which payload broke, then rethrows.
   * WHY: Netty reports codec errors without the payload id; upstream logged the packet class the same way.
   * @param type   Payload type, for the log message
   * @param codec  Codec to wrap
   * @param <B>  Buffer type
   * @param <T>  Payload class
   * @return  Codec with the same encoding
   */
  public static <B, T> StreamCodec<B, T> logErrors(CustomPacketPayload.Type<?> type, StreamCodec<? super B, T> codec) {
    return new StreamCodec<>() {
      @Override
      public T decode(B buffer) {
        try {
          return codec.decode(buffer);
        } catch (RuntimeException e) {
          Mantle.logger.error("Exception while decoding payload {}", type.id(), e);
          throw e;
        }
      }

      @Override
      public void encode(B buffer, T value) {
        try {
          codec.encode(buffer, value);
        } catch (RuntimeException e) {
          Mantle.logger.error("Exception while encoding payload {}", type.id(), e);
          throw e;
        }
      }
    };
  }


  /* Sending */

  /**
   * Sends a payload to a player; does nothing for client side players.
   * @param payload  Payload to send
   * @param player   Receiving player
   */
  public void sendTo(CustomPacketPayload payload, Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      sendTo(payload, serverPlayer);
    }
  }

  /**
   * Sends a payload to a player; does nothing for fake players, which have no client.
   * @param payload  Payload to send
   * @param player   Receiving player
   */
  public void sendTo(CustomPacketPayload payload, ServerPlayer player) {
    requireClientbound(payload);
    if (!(player instanceof FakePlayer)) {
      deliver(player, payload);
    }
  }

  /**
   * Sends a payload to every player that has the position's chunk loaded.
   * @param payload  Payload to send
   * @param level    Level holding the position
   * @param pos      Position the payload is about
   */
  public void sendToClientsAround(CustomPacketPayload payload, ServerLevel level, BlockPos pos) {
    sendToAll(payload, PlayerLookup.tracking(level, pos));
  }

  /**
   * Sends a payload to every player tracking the entity.
   * @param payload  Payload to send
   * @param entity   Tracked entity, on the server
   */
  public void sendToTracking(CustomPacketPayload payload, Entity entity) {
    sendToAll(payload, PlayerLookup.tracking(entity));
  }

  /**
   * Sends a payload to every player tracking the entity, and to the entity itself if it is a player.
   * @param payload  Payload to send
   * @param entity   Tracked entity, on the server
   */
  public void sendToTrackingAndSelf(CustomPacketPayload payload, Entity entity) {
    Collection<ServerPlayer> tracking = PlayerLookup.tracking(entity);
    sendToAll(payload, tracking);
    // WHY: PlayerLookup does not promise to leave a player out of its own tracking list, so avoid sending twice
    if (entity instanceof ServerPlayer self && !tracking.contains(self)) {
      sendTo(payload, self);
    }
  }

  /**
   * Sends a vanilla packet to a player; does nothing for other entities.
   * @param packet  Packet to send
   * @param entity  Receiving player
   */
  public void sendVanillaPacket(Packet<?> packet, Entity entity) {
    if (entity instanceof ServerPlayer player) {
      player.connection.send(packet);
    }
  }

  /** Sends a payload to each player */
  private void sendToAll(CustomPacketPayload payload, Collection<ServerPlayer> players) {
    for (ServerPlayer player : players) {
      sendTo(payload, player);
    }
  }

  /** Fails when the payload was not registered as clientbound here */
  private void requireClientbound(CustomPacketPayload payload) {
    Identifier id = payload.type().id();
    if (!clientbound.contains(id)) {
      throw new IllegalArgumentException("Payload " + id + " is not registered as clientbound on network '" + modId + "'; call registerClientbound from the common entrypoint");
    }
  }

  /**
   * Hands one payload to Fabric for one real player.
   * Overridden in tests to record deliveries instead of sending them.
   * @param player   Receiving player, never a fake player
   * @param payload  Registered clientbound payload
   */
  protected void deliver(ServerPlayer player, CustomPacketPayload payload) {
    ServerPlayNetworking.send(player, payload);
  }


  /* Connection helpers */

  /**
   * Checks if the player's client runs in this game instance, as the host of a single player world does.
   * Such a client shares the server's loaded data, so datapack sync payloads are not needed.
   * @param player  Player to check
   * @return  True for an in-memory connection, false for a network connection (including LAN guests)
   */
  public static boolean isMemoryConnection(ServerPlayer player) {
    return player.connection.getPacketContext().orElseThrow(PacketContext.CONNECTION).isMemoryConnection();
  }
}
