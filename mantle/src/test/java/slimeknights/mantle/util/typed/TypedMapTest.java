package slimeknights.mantle.util.typed;

import org.junit.jupiter.api.Test;
import slimeknights.mantle.util.typed.MutableTypedMap.ComputingKey;
import slimeknights.mantle.util.typed.TypedMap.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks typed maps */
class TypedMapTest {
  private static final Key<String> NAME = new Key<>() {};
  private static final Key<Integer> COUNT = new Key<>() {};

  @Test
  void emptyMapReturnsTheDefault() {
    // upstream returned null here
    assertEquals("fallback", TypedMap.EMPTY.getOrDefault(NAME, "fallback"));
    assertNull(TypedMap.EMPTY.get(NAME));
    assertTrue(TypedMap.EMPTY.isEmpty());
  }

  @Test
  void builderKeepsValuesAndIsImmutable() {
    TypedMap map = TypedMapBuilder.builder().put(NAME, "gem").put(COUNT, 3).build();
    assertEquals("gem", map.get(NAME));
    assertEquals(3, map.getOrDefault(COUNT, 0));
    assertEquals(2, map.size());
    assertSame(TypedMap.EMPTY, TypedMapBuilder.builder().build());
    assertThrows(UnsupportedOperationException.class, () -> ((MutableTypedMap) map).put(NAME, "other"));
  }

  @Test
  void mutableMapComputesMissingValuesOnce() {
    int[] calls = {0};
    ComputingKey<String> lazy = () -> {
      calls[0]++;
      return "computed";
    };
    BackedTypedMap map = new BackedTypedMap();
    assertEquals("computed", map.computeIfAbsent(lazy));
    assertEquals("computed", map.computeIfAbsent(lazy));
    assertEquals(1, calls[0]);
    map.remove(lazy);
    assertTrue(map.isEmpty());
  }
}
