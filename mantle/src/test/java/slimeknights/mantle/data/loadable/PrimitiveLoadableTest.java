package slimeknights.mantle.data.loadable;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.CharacterLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.LongLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slimeknights.mantle.data.loadable.LoadableTesting.json;
import static slimeknights.mantle.data.loadable.LoadableTesting.network;

/** Checks the primitive loadables: JSON, network, ranges and errors */
class PrimitiveLoadableTest {
  enum Color { RED, DARK_BLUE }

  @Test
  void intRoundTripsAndPicksCompactNetworkForm() {
    assertEquals(42, json(IntLoadable.FROM_ZERO, 42));
    assertEquals(-5, network(IntLoadable.ANY_BYTE, -5));
    assertEquals(Integer.MIN_VALUE, network(IntLoadable.ANY_FULL, Integer.MIN_VALUE));
    assertEquals(IntLoadable.IntNetwork.VAR_INT, IntLoadable.IntNetwork.recommended(0, 100));
    assertEquals(IntLoadable.IntNetwork.BYTE, IntLoadable.IntNetwork.recommended(-1, 100));
    assertEquals(IntLoadable.IntNetwork.SHORT, IntLoadable.IntNetwork.recommended(-1, Short.MAX_VALUE));
    assertEquals(IntLoadable.IntNetwork.INT, IntLoadable.IntNetwork.recommended(-1, Integer.MAX_VALUE));
  }

  @Test
  void rangeErrorsNameOnlyTheBoundsThatApply() {
    JsonSyntaxException low = assertThrows(JsonSyntaxException.class, () -> IntLoadable.FROM_ONE.convert(new JsonPrimitive(0), "count"));
    assertEquals("count must not be less than 1", low.getMessage());
    JsonSyntaxException range = assertThrows(JsonSyntaxException.class, () -> FloatLoadable.PERCENT.convert(new JsonPrimitive(2), "chance"));
    assertEquals("chance must be between 0.0 and 1.0", range.getMessage());
    JsonSyntaxException high = assertThrows(JsonSyntaxException.class, () -> IntLoadable.range(Integer.MIN_VALUE, 5).convert(new JsonPrimitive(6), "x"));
    assertEquals("x must not be greater than 5", high.getMessage());
  }

  @Test
  void numbersAsStringsUseTheRadix() {
    StringLoadable<Integer> hex = IntLoadable.FROM_ZERO.asString(16);
    assertEquals("ff", hex.getString(255));
    assertEquals(255, hex.parseString("ff", "color"));
    assertEquals(new JsonPrimitive("ff"), hex.serialize(255));
    assertEquals(1L << 40, json(LongLoadable.ANY.asString(10), 1L << 40));
    assertThrows(JsonSyntaxException.class, () -> hex.parseString("zz", "color"));
  }

  @Test
  void booleanStringIsStrict() {
    assertEquals(true, BooleanLoadable.INSTANCE.parseString("TRUE", "flag"));
    assertThrows(JsonSyntaxException.class, () -> BooleanLoadable.INSTANCE.parseString("yes", "flag"));
    assertEquals(false, network(BooleanLoadable.INSTANCE, false));
  }

  @Test
  void stringsAndCharactersRespectLimits() {
    StringLoadable<String> short5 = StringLoadable.maxLength(5);
    assertEquals("abc", json(short5, "abc"));
    assertThrows(JsonSyntaxException.class, () -> short5.convert(new JsonPrimitive("toolong"), "name"));
    assertEquals("hello", network(StringLoadable.DEFAULT, "hello"));
    assertEquals('x', json(CharacterLoadable.INSTANCE, 'x'));
    assertThrows(JsonSyntaxException.class, () -> CharacterLoadable.INSTANCE.parseString("xy", "symbol"));
  }

  @Test
  void enumsUseLowercaseNamesAndOrdinals() {
    EnumLoadable<Color> colors = new EnumLoadable<>(Color.class);
    assertEquals(new JsonPrimitive("dark_blue"), colors.serialize(Color.DARK_BLUE));
    assertEquals(Color.DARK_BLUE, network(colors, Color.DARK_BLUE));
    assertThrows(JsonSyntaxException.class, () -> EnumLoadable.of(Color.RED).parseString("dark_blue", "color"));
    RegistryFriendlyByteBuf buffer = LoadableTesting.buffer();
    buffer.writeVarInt(7);
    assertThrows(DecoderException.class, () -> colors.decode(buffer));
  }

  @Test
  void identifiersParseWithKeyedErrors() {
    Identifier id = Identifier.fromNamespaceAndPath("mantle", "test");
    assertEquals(id, json(IdentifierLoadable.DEFAULT, id));
    assertEquals(id, network(IdentifierLoadable.DEFAULT, id));
    JsonSyntaxException error = assertThrows(JsonSyntaxException.class, () -> IdentifierLoadable.DEFAULT.parseString("Bad Id!", "item"));
    assertTrue(error.getMessage().startsWith("Expected item to be an identifier"), error.getMessage());
  }
}
