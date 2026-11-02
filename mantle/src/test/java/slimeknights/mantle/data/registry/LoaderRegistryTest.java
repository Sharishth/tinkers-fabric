package slimeknights.mantle.data.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.gson.IdentifierSerializer;
import slimeknights.mantle.data.gson.TagKeySerializer;
import slimeknights.mantle.data.loadable.LoadableTesting;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks loader registries, named component registries and the Gson serializers */
class LoaderRegistryTest {
  interface Shape extends IHaveLoader {}

  record Circle(int radius) implements Shape {
    static final RecordLoadable<Circle> LOADER = RecordLoadable.create(IntLoadable.FROM_ONE.requiredField("radius", Circle::radius), Circle::new);

    @Override
    public RecordLoadable<Circle> getLoader() {
      return LOADER;
    }
  }

  /** Shape with no fields, written compactly as its type */
  enum Dot implements Shape {
    INSTANCE;

    static final RecordLoadable<Dot> LOADER = new SingletonLoader<>(INSTANCE);

    @Override
    public RecordLoadable<Dot> getLoader() {
      return LOADER;
    }
  }

  private static final Identifier CIRCLE = Identifier.fromNamespaceAndPath("mantle_test", "circle");
  private static final Identifier DOT = Identifier.fromNamespaceAndPath("mantle_test", "dot");

  private static GenericLoaderRegistry<Shape> shapes() {
    GenericLoaderRegistry<Shape> registry = new GenericLoaderRegistry<>("shape", true);
    registry.register(CIRCLE, Circle.LOADER);
    registry.register(DOT, Dot.LOADER);
    return registry;
  }

  @Test
  void typeKeyPicksTheLoader() {
    GenericLoaderRegistry<Shape> registry = shapes();
    assertEquals(new Circle(3), registry.convert(json("{\"type\": \"mantle_test:circle\", \"radius\": 3}"), "shape"));
    assertEquals(json("{\"type\": \"mantle_test:circle\", \"radius\": 3}"), registry.serialize(new Circle(3)));
    assertSame(Dot.INSTANCE, registry.convert(json("\"mantle_test:dot\""), "shape"));
    assertEquals(json("\"mantle_test:dot\""), registry.serialize(Dot.INSTANCE));
    JsonSyntaxException unknown = assertThrows(JsonSyntaxException.class, () -> registry.convert(json("{\"type\": \"mantle_test:square\"}"), "shape"));
    assertTrue(unknown.getMessage().startsWith("Unknown shape loader mantle_test:square"), unknown.getMessage());
    assertThrows(JsonSyntaxException.class, () -> new GenericLoaderRegistry<Shape>("shape", false).convert(json("\"mantle_test:dot\""), "shape"));
  }

  @Test
  void networkSendsTheTypeId() {
    GenericLoaderRegistry<Shape> registry = shapes();
    assertEquals(new Circle(4), network(registry, new Circle(4)));
    assertSame(Dot.INSTANCE, network(registry, Dot.INSTANCE));
  }

  @Test
  void defaultingRegistryUsesTheDefaultForNullAndEmptyIds() {
    DefaultingLoaderRegistry<Shape> registry = new DefaultingLoaderRegistry<>("shape", Dot.INSTANCE, false);
    registry.register(CIRCLE, Circle.LOADER);
    assertSame(Dot.INSTANCE, registry.convert(json("null"), "shape"));
    assertSame(Dot.INSTANCE, registry.getOrDefault(new JsonObject(), "shape"));
    assertSame(Dot.INSTANCE, network(registry, Dot.INSTANCE));
    assertEquals(new Circle(2), network(registry, new Circle(2)));
  }

  @Test
  void mergingFieldSharesTheParentObject() {
    record Holder(Shape shape, int count) {}
    GenericLoaderRegistry<Shape> registry = shapes();
    RecordLoadable<Holder> holder = RecordLoadable.create(
      registry.directField("shape_type", Holder::shape), IntLoadable.FROM_ONE.requiredField("count", Holder::count), Holder::new);
    JsonObject parent = json("{\"shape_type\": \"mantle_test:circle\", \"radius\": 5, \"count\": 2}").getAsJsonObject();
    assertEquals(new Holder(new Circle(5), 2), holder.deserialize(parent));
    assertTrue(parent.has("shape_type") && !parent.has("type"), "parent JSON left unchanged: " + parent);
    assertEquals(parent, holder.serialize(new Holder(new Circle(5), 2)));
  }

  @Test
  void namedComponentsRejectDuplicatesAndSendNullAsEmpty() {
    NamedComponentRegistry<String> names = new NamedComponentRegistry<>("Unknown name");
    names.register(CIRCLE, "circle");
    assertThrows(IllegalArgumentException.class, () -> names.register(CIRCLE, "other"));
    assertThrows(IllegalArgumentException.class, () -> names.register(DOT, "circle"));
    var buffer = LoadableTesting.buffer();
    names.encodeOptional(buffer, null);
    assertNull(names.decodeOptional(buffer));
    buffer.writeUtf("mantle_test:missing");
    assertThrows(DecoderException.class, () -> names.decodeOptional(buffer));
  }

  @Test
  void gsonSerializersFillTheNamespace() {
    Gson gson = new GsonBuilder()
      .registerTypeAdapter(Identifier.class, IdentifierSerializer.identifier("mantle"))
      .registerTypeAdapter(new TypeToken<TagKey<Item>>() {}.getType(), new TagKeySerializer<>(Registries.ITEM))
      .create();
    assertEquals(Identifier.fromNamespaceAndPath("mantle", "book"), gson.fromJson("\"book\"", Identifier.class));
    assertEquals(Identifier.fromNamespaceAndPath("other", "book"), gson.fromJson("\"other:book\"", Identifier.class));
    TagKey<Item> logs = gson.fromJson("\"minecraft:logs\"", new TypeToken<TagKey<Item>>() {}.getType());
    assertEquals(Identifier.withDefaultNamespace("logs"), logs.location());
    assertThrows(JsonSyntaxException.class, () -> gson.fromJson("\"Bad Id\"", Identifier.class));
  }
}
