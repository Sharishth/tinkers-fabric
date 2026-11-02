package slimeknights.mantle.registration.object;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks EnumObject keeps upstream semantics. */
class EnumObjectTest {
  private enum Color { RED, GREEN, BLUE }

  /** Mimics an unregistered RegistryObject, which throws NullPointerException */
  private static String unbound() {
    throw new NullPointerException("not registered");
  }

  private static EnumObject<Color, String> sample() {
    return new EnumObject.Builder<Color, String>(Color.class)
      .put(Color.RED, "red")
      .put(Color.GREEN, EnumObjectTest::unbound)
      .build();
  }

  @Test
  void getReturnsValue() {
    assertEquals("red", sample().get(Color.RED));
  }

  @Test
  void getMissingKeyThrows() {
    assertThrows(NoSuchElementException.class, () -> sample().get(Color.BLUE));
  }

  @Test
  void getOrNullHandlesMissingAndUnbound() {
    EnumObject<Color, String> object = sample();
    assertNull(object.getOrNull(Color.BLUE));
    assertNull(object.getOrNull(Color.GREEN));
  }

  @Test
  void forEachSkipsUnboundEntries() {
    List<String> seen = new ArrayList<>();
    sample().forEach((key, value) -> seen.add(key + "=" + value));
    assertEquals(List.of("RED=red"), seen);
  }

  @Test
  void keysKeepEnumOrder() {
    EnumObject<Color, String> object = new EnumObject.Builder<Color, String>(Color.class)
      .put(Color.BLUE, "b").put(Color.RED, "r").build();
    assertEquals(List.of(Color.RED, Color.BLUE), List.copyOf(object.keys()));
    assertEquals(List.of("r", "b"), object.values());
  }

  @Test
  void containsChecksValues() {
    EnumObject<Color, String> object = new EnumObject.Builder<Color, String>(Color.class).put(Color.RED, "red").build();
    assertTrue(object.contains("red"));
    assertFalse(object.contains("blue"));
  }

  @Test
  void emptyHasNoKeys() {
    assertTrue(EnumObject.<Color, String>empty().keys().isEmpty());
  }
}
