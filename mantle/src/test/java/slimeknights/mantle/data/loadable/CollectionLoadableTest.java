package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks arrays, lists, sets and maps */
class CollectionLoadableTest {
  enum Side { UP, DOWN, NORTH }

  @Test
  void positiveMinimumSizeIsEnforced() {
    // upstream's SizeRange turned any non-compact minimum into 0
    ArrayLoadable<int[]> pair = IntLoadable.ANY_FULL.array(2, 2);
    assertThrows(JsonSyntaxException.class, () -> pair.convert(json("[]"), "pair"));
    assertThrows(JsonSyntaxException.class, () -> pair.convert(json("[1]"), "pair"));
    assertThrows(JsonSyntaxException.class, () -> pair.convert(json("[1, 2, 3]"), "pair"));
    assertArrayEquals(new int[] {1, 2}, pair.convert(json("[1, 2]"), "pair"));
    assertEquals(0, ArrayLoadable.SizeRange.effectiveMinSize(ArrayLoadable.COMPACT_OR_EMPTY));
    assertEquals(1, ArrayLoadable.SizeRange.effectiveMinSize(ArrayLoadable.COMPACT));
  }

  @Test
  void compactListsAcceptAndWriteSingleElements() {
    Loadable<List<String>> names = StringLoadable.DEFAULT.list(ArrayLoadable.COMPACT);
    assertEquals(List.of("one"), names.convert(json("\"one\""), "names"));
    assertEquals(json("\"one\""), names.serialize(List.of("one")));
    assertEquals(json("[\"a\", \"b\"]"), names.serialize(List.of("a", "b")));
    assertThrows(JsonSyntaxException.class, () -> StringLoadable.DEFAULT.list(1).convert(json("\"one\""), "names"));
    assertThrows(JsonSyntaxException.class, () -> names.convert(json("[]"), "names"));
  }

  @Test
  void collectionsRoundTrip() {
    assertEquals(List.of(3, 1, 2), network(IntLoadable.ANY_FULL.list(0), List.of(3, 1, 2)));
    assertEquals(Set.of("x", "y"), json(StringLoadable.DEFAULT.set(), Set.of("x", "y")));
    assertArrayEquals(new short[] {1, -2}, network(IntLoadable.ANY_SHORT.shortArray(0), new short[] {1, -2}));
    assertArrayEquals(new String[] {"a", null}, json(StringLoadable.DEFAULT.array(String[]::new, true, 0), new String[] {"a", null}));
    assertArrayEquals(new String[] {null, "b"}, network(StringLoadable.DEFAULT.array(String[]::new, true, 0), new String[] {null, "b"}));
  }

  @Test
  void enumSetsAndMapsUseEnumCollections() {
    EnumLoadable<Side> sides = new EnumLoadable<>(Side.class);
    Set<Side> read = sides.set(ArrayLoadable.COMPACT).convert(json("\"up\""), "sides");
    assertEquals(EnumSet.of(Side.UP), read);
    assertEquals(Set.of(Side.DOWN, Side.NORTH), network(sides.set(), EnumSet.of(Side.DOWN, Side.NORTH)));
    Map<Side, Integer> weights = Map.of(Side.UP, 2, Side.NORTH, 5);
    assertEquals(weights, json(sides.mapWithValues(IntLoadable.FROM_ZERO), weights));
    assertEquals(weights, network(sides.mapWithValues(IntLoadable.FROM_ZERO), weights));
  }

  @Test
  void stringMapsCheckSize() {
    Loadable<Map<String, Integer>> counts = StringLoadable.DEFAULT.mapWithValues(IntLoadable.FROM_ZERO);
    assertEquals(Map.of("a", 1), counts.convert(json("{\"a\": 1}"), "counts"));
    JsonSyntaxException empty = assertThrows(JsonSyntaxException.class, () -> counts.convert(json("{}"), "counts"));
    assertTrue(empty.getMessage().contains("at least 1"), empty.getMessage());
  }

  @Test
  void mapsKeyedFromValuesRoundTrip() {
    Loadable<Map<Integer, String>> byLength = StringLoadable.DEFAULT.mapWithKeys(1, String::length);
    assertEquals(Map.of(1, "a", 3, "abc"), byLength.convert(json("[\"a\", \"abc\"]"), "words"));
  }
}
