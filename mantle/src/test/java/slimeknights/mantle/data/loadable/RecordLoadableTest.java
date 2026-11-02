package slimeknights.mantle.data.loadable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LegacyField;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks record loadables and their fields */
class RecordLoadableTest {
  enum Shape { ROUND, SQUARE }

  record Gem(String name, int size, boolean shiny, @Nullable Shape shape) {}

  static final RecordLoadable<Gem> GEM = RecordLoadable.create(
    StringLoadable.DEFAULT.requiredField("name", Gem::name),
    IntLoadable.FROM_ONE.defaultField("size", 1, Gem::size),
    BooleanLoadable.INSTANCE.defaultField("shiny", false, Gem::shiny),
    new EnumLoadable<>(Shape.class).nullableField("shape", Gem::shape),
    Gem::new);

  @Test
  void requiredDefaultAndNullableFieldsReadJson() {
    Gem gem = GEM.convert(LoadableTesting.json("{\"name\": \"ruby\", \"shape\": \"square\"}"), "gem");
    assertEquals(new Gem("ruby", 1, false, Shape.SQUARE), gem);
    JsonSyntaxException missing = assertThrows(JsonSyntaxException.class, () -> GEM.convert(LoadableTesting.json("{}"), "gem"));
    assertEquals("Missing JSON field 'name'", missing.getMessage());
  }

  @Test
  void defaultsAreSkippedExceptBooleansAndNullsAreLeftOut() {
    JsonObject written = GEM.serialize(new Gem("opal", 1, false, null)).getAsJsonObject();
    assertFalse(written.has("size"), "default int left out");
    assertTrue(written.has("shiny"), "booleans always written");
    assertFalse(written.has("shape"), "null left out");
    assertEquals(new Gem("opal", 3, true, null), json(GEM, new Gem("opal", 3, true, null)));
  }

  @Test
  void networkRoundTripKeepsNulls() {
    assertEquals(new Gem("jade", 2, true, null), network(GEM, new Gem("jade", 2, true, null)));
    assertEquals(new Gem("jade", 2, true, Shape.ROUND), network(GEM, new Gem("jade", 2, true, Shape.ROUND)));
  }

  @Test
  void loadableIsAStreamCodecAndACodec() {
    // D-20: any loadable can be handed to vanilla code that wants a StreamCodec
    StreamCodec<RegistryFriendlyByteBuf, String> names = GEM.map(Gem::name, name -> new Gem(name, 1, false, null));
    assertEquals("pearl", network(names, "pearl"));
    LoadableCodec<Gem> codec = new LoadableCodec<>(GEM);
    Gem gem = new Gem("amber", 4, false, Shape.ROUND);
    assertEquals(gem, codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, gem).getOrThrow()).getOrThrow());
  }

  @Test
  void mappingErrorsMatchWhereTheyRun() {
    Loadable<Integer> even = IntLoadable.ANY_FULL.validate((value, error) -> {
      if (value % 2 != 0) {
        throw error.create("must be even");
      }
      return value;
    });
    assertThrows(JsonSyntaxException.class, () -> even.convert(LoadableTesting.json("3"), "x"));
    RegistryFriendlyByteBuf buffer = LoadableTesting.buffer();
    IntLoadable.ANY_FULL.encode(buffer, 3);
    assertThrows(DecoderException.class, () -> even.decode(buffer));
  }

  @Test
  void legacyFieldReadsTheOldKey() {
    RecordLoadable<Gem> renamed = RecordLoadable.create(
      new LegacyField<>(StringLoadable.DEFAULT.requiredField("name", Gem::name), "id"),
      name -> new Gem(name, 1, false, null));
    assertEquals("old", renamed.convert(LoadableTesting.json("{\"id\": \"old\"}"), "gem").name());
    assertEquals("new", renamed.convert(LoadableTesting.json("{\"id\": \"old\", \"name\": \"new\"}"), "gem").name());
    assertThrows(IllegalArgumentException.class, () -> new LegacyField<>(StringLoadable.DEFAULT.requiredField("name", Gem::name), "name"));
  }

  @Test
  void contextFieldsComeFromContext() {
    record Named(String id) {}
    RecordLoadable<Named> named = RecordLoadable.create(ContextKey.DEBUG.requiredField(), Named::new);
    var context = TypedMapBuilder.builder().put(ContextKey.DEBUG, "from context").build();
    assertEquals("from context", named.deserialize(new JsonObject(), context).id());
    assertThrows(JsonSyntaxException.class, () -> named.deserialize(new JsonObject()));
    assertEquals(0, named.serialize(new Named("x")).getAsJsonObject().size(), "context fields are never written");
  }

  @Test
  void tryDirectFieldWritesIntoTheParentWhenUnambiguous() {
    record Holder(Gem gem, int count) {}
    RecordLoadable<Holder> holder = RecordLoadable.create(
      GEM.tryDirectField("gem", Holder::gem),
      IntLoadable.FROM_ONE.requiredField("count", Holder::count),
      Holder::new);
    Holder value = new Holder(new Gem("onyx", 2, false, null), 5);
    JsonObject written = holder.serialize(value).getAsJsonObject();
    assertTrue(written.has("name") && !written.has("gem"), "gem keys written directly: " + written);
    assertEquals(value, holder.convert(written, "holder"));
    assertEquals(value, holder.convert(LoadableTesting.json("{\"gem\": {\"name\": \"onyx\", \"size\": 2}, \"count\": 5}"), "holder"));
  }

  @Test
  void withLoaderAndSingletonPassTheLoader() {
    record Self(int value, RecordLoadable<Self> loader) {}
    RecordLoadable<Self> self = RecordLoadable.withLoader(IntLoadable.ANY_FULL.requiredField("value", Self::value), Self::new);
    assertSame(self, self.convert(LoadableTesting.json("{\"value\": 1}"), "self").loader());
    record Unit(RecordLoadable<Unit> loader) {}
    Unit unit = SingletonLoader.singleton(Unit::new);
    assertSame(unit, unit.loader().convert(new JsonObject(), "unit"));
    assertSame(unit, network(unit.loader(), unit));
  }

  @Test
  void compactFormReadsNonObjects() {
    RecordLoadable<Gem> compact = GEM.compact(StringLoadable.DEFAULT.map(name -> new Gem(name, 1, false, null), Gem::name),
                                              gem -> gem.size() == 1 && !gem.shiny() && gem.shape() == null);
    assertEquals("ruby", compact.convert(LoadableTesting.json("\"ruby\""), "gem").name());
    assertEquals(LoadableTesting.json("\"ruby\""), compact.serialize(new Gem("ruby", 1, false, null)));
    assertTrue(compact.serialize(new Gem("ruby", 2, false, null)).isJsonObject());
    assertNull(compact.convert(LoadableTesting.json("{\"name\": \"x\"}"), "gem").shape());
  }
}
