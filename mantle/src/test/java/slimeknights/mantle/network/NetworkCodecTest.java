package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.LoadableTesting;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.network.packet.IServerboundPacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks payload codecs as registered, and the wrapper's registration guards */
class NetworkCodecTest {
  /** Payload whose codec is a Mantle loadable, as Tinkers' datapack sync payloads will be */
  record SamplePayload(String name, int count) implements IServerboundPacket {
    static final Type<SamplePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mantle", "sample"));
    static final RecordLoadable<SamplePayload> LOADABLE = RecordLoadable.create(
      StringLoadable.DEFAULT.requiredField("name", SamplePayload::name),
      IntLoadable.ANY_FULL.requiredField("count", SamplePayload::count),
      SamplePayload::new);

    @Override
    public Type<SamplePayload> type() {
      return TYPE;
    }

    @Override
    public void handle(ServerPlayer sender) {}
  }

  /** Builds the codec exactly as NetworkWrapper registers it */
  private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> registered(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    return NetworkWrapper.logErrors(type, codec);
  }

  @Test
  void dropLecternBookRoundTrips() {
    DropLecternBookPacket packet = new DropLecternBookPacket(new BlockPos(12, -60, -3_000_000));
    assertEquals(packet, LoadableTesting.network(registered(DropLecternBookPacket.TYPE, DropLecternBookPacket.STREAM_CODEC), packet));
  }

  @Test
  void loadableIsAPayloadCodecWithoutCasts() {
    // D-20: a RecordLoadable is a StreamCodec<RegistryFriendlyByteBuf, T>, so it fits payload registration directly
    StreamCodec<? super RegistryFriendlyByteBuf, SamplePayload> codec = SamplePayload.LOADABLE;
    SamplePayload payload = new SamplePayload("ingot", -90);
    assertEquals(payload, LoadableTesting.network(registered(SamplePayload.TYPE, codec), payload));
  }

  @Test
  void loggedCodecRethrowsTheOriginalFailure() {
    StreamCodec<RegistryFriendlyByteBuf, SamplePayload> codec = registered(SamplePayload.TYPE, SamplePayload.LOADABLE);
    // an empty buffer cannot hold a payload
    assertThrows(IndexOutOfBoundsException.class, () -> codec.decode(LoadableTesting.buffer()));

    IllegalStateException failure = new IllegalStateException("broken encoder");
    StreamCodec<RegistryFriendlyByteBuf, SamplePayload> broken = registered(SamplePayload.TYPE, StreamCodec.of(
      (buffer, value) -> { throw failure; },
      buffer -> new SamplePayload("unused", 0)));
    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> broken.encode(LoadableTesting.buffer(), new SamplePayload("x", 1)));
    assertSame(failure, thrown);
  }

  @Test
  void registrationRejectsIdsOfOtherMods() {
    // the namespace check runs before anything reaches Fabric's global payload registry
    NetworkWrapper network = new NetworkWrapper("tconstruct");
    IllegalArgumentException clientbound = assertThrows(IllegalArgumentException.class, () -> network.registerClientbound(SamplePayload.TYPE, SamplePayload.LOADABLE));
    assertTrue(clientbound.getMessage().contains("mantle:sample"), clientbound.getMessage());
    assertThrows(IllegalArgumentException.class, () -> network.registerServerbound(SamplePayload.TYPE, SamplePayload.LOADABLE));
  }
}
