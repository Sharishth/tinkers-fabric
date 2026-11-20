package slimeknights.tconstruct.gametest.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.BlockEntityPacket;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Test payload for {@link NetworkClientGameTest}, registered from the common entrypoint like a real one.
 * Its client receiver is registered by {@link NetworkTestClient}.
 */
public class NetworkTestContent implements ModInitializer {
  /** Id of this test mod, from its fabric.mod.json */
  private static final String MOD_ID = "tconstruct-gametest";

  /** Network for this test mod's payloads */
  public static final NetworkWrapper NETWORK = new NetworkWrapper(MOD_ID);
  /** Last message a lectern received on the client; null until one arrives */
  public static final AtomicReference<String> RECEIVED = new AtomicReference<>();

  /**
   * Clientbound payload that delivers a message to a lectern block entity on the client.
   * @param pos      Lectern position
   * @param message  Message stored in {@link #RECEIVED} when handled
   */
  public record LecternPingPacket(BlockPos pos, String message) implements BlockEntityPacket<LecternBlockEntity> {
    public static final Type<LecternPingPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "lectern_ping"));
    public static final StreamCodec<ByteBuf, LecternPingPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, LecternPingPacket::pos,
      ByteBufCodecs.STRING_UTF8, LecternPingPacket::message,
      LecternPingPacket::new);

    @Override
    public Class<LecternBlockEntity> blockEntityClass() {
      return LecternBlockEntity.class;
    }

    @Override
    public void handleBlockEntity(LecternBlockEntity be) {
      RECEIVED.set(message);
    }

    @Override
    public Type<LecternPingPacket> type() {
      return TYPE;
    }
  }

  @Override
  public void onInitialize() {
    NETWORK.registerClientbound(LecternPingPacket.TYPE, LecternPingPacket.STREAM_CODEC);
  }
}
