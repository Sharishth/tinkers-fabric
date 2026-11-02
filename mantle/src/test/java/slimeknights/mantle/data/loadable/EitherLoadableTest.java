package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.mapping.EitherLoadable;
import slimeknights.mantle.data.loadable.mapping.SimpleRecordLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks EitherLoadable and SimpleRecordLoadable */
class EitherLoadableTest {
  /** Shape that knows its loader */
  sealed interface Shape extends IAmLoadable.Record permits Circle, Square {}

  record Circle(int radius) implements Shape {
    static final RecordLoadable<Circle> LOADER = RecordLoadable.create(IntLoadable.FROM_ONE.requiredField("radius", Circle::radius), Circle::new);

    @Override
    public RecordLoadable<Circle> loadable() {
      return LOADER;
    }
  }

  record Square(int side) implements Shape {
    static final RecordLoadable<Square> LOADER = RecordLoadable.create(IntLoadable.FROM_ONE.requiredField("side", Square::side), Square::new);

    @Override
    public RecordLoadable<Square> loadable() {
      return LOADER;
    }
  }

  static final RecordLoadable<Shape> SHAPES = EitherLoadable.<Shape>record().key("radius", Circle.LOADER).key("side", Square.LOADER).build();

  @Test
  void keyPicksTheOption() {
    assertEquals(new Circle(2), SHAPES.convert(json("{\"radius\": 2}"), "shape"));
    assertEquals(new Square(3), SHAPES.convert(json("{\"side\": 3}"), "shape"));
    assertEquals(json("{\"side\": 3}"), SHAPES.serialize(new Square(3)));
    JsonSyntaxException error = assertThrows(JsonSyntaxException.class, () -> SHAPES.convert(json("{\"corners\": 3}"), "shape"));
    assertEquals("JSON at [root] must be one of: object with key from [radius, side]", error.getMessage());
  }

  @Test
  void networkSendsTheOptionIndex() {
    assertEquals(new Circle(5), network(SHAPES, new Circle(5)));
    assertEquals(new Square(7), network(SHAPES, new Square(7)));
  }

  @Test
  void typedFormAcceptsPrimitives() {
    Loadable<Shape> shapes = EitherLoadable.<Shape>typed()
      .primitive(IntLoadable.FROM_ONE.map(Circle::new, circle -> ((Circle) circle).radius()))
      .key("side", Square.LOADER)
      .build(SHAPES);
    assertEquals(new Circle(4), shapes.convert(json("4"), "shape"));
    assertEquals(new Square(4), shapes.convert(json("{\"side\": 4}"), "shape"));
    assertEquals(new Circle(9), network(shapes, new Circle(9)));
    JsonSyntaxException error = assertThrows(JsonSyntaxException.class, () -> shapes.convert(json("[1]"), "shape"));
    assertEquals("JSON at shape must be one of: primitive, object with key from [side]", error.getMessage());
  }

  @Test
  void needsTwoOptions() {
    assertThrows(IllegalStateException.class, () -> EitherLoadable.<Shape>record().key("radius", Circle.LOADER).build());
    Loadable<Shape> circleFromInt = IntLoadable.FROM_ONE.map(Circle::new, circle -> ((Circle) circle).radius());
    assertThrows(IllegalStateException.class, () -> EitherLoadable.<Shape>typed().primitive(circleFromInt).primitive(circleFromInt));
  }

  @Test
  void simpleRecordReadsBothForms() {
    SimpleRecordLoadable<Integer> wrapped = new SimpleRecordLoadable<>(IntLoadable.FROM_ZERO, "amount", 1, false);
    assertEquals(5, wrapped.convert(json("5"), "amount"));
    assertEquals(6, wrapped.convert(json("{\"amount\": 6}"), "amount"));
    assertEquals(1, wrapped.convert(json("{}"), "amount"), "default when the key is missing");
    assertEquals(json("{\"amount\": 5}"), wrapped.serialize(5));
    assertEquals(json("5"), new SimpleRecordLoadable<>(IntLoadable.FROM_ZERO, "amount", null, true).serialize(5));
    assertThrows(JsonSyntaxException.class, () -> new SimpleRecordLoadable<>(IntLoadable.FROM_ZERO, "amount", null, true).convert(json("{}"), "x"));
    assertEquals(8, network(wrapped, 8));
  }
}
