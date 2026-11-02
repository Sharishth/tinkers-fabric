package slimeknights.mantle;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Checks Mantle's id and translation helpers. */
class MantleTest {
  @Test
  void getResourceUsesMantleNamespace() {
    Identifier id = Mantle.getResource("books/test");
    assertEquals("mantle", id.getNamespace());
    assertEquals("books/test", id.getPath());
  }

  @Test
  void commonResourceUsesConventionalNamespace() {
    Identifier id = Mantle.commonResource("ingots/iron");
    assertEquals("c", id.getNamespace());
    assertEquals("ingots/iron", id.getPath());
  }

  @Test
  void makeDescriptionIdMatchesUpstreamFormat() {
    // upstream format: <base>.<namespace>.<path>, slashes become dots
    assertEquals("gui.mantle.book", Mantle.makeDescriptionId("gui", "book"));
    assertEquals("block.mantle.sub.name", Mantle.makeDescriptionId("block", "sub/name"));
  }

  @Test
  void makeComponentCarriesKeyAndArgs() {
    MutableComponent component = Mantle.makeComponent("gui", "page", 3, "x");
    TranslatableContents contents = assertInstanceOf(TranslatableContents.class, component.getContents());
    assertEquals("gui.mantle.page", contents.getKey());
    assertArrayEquals(new Object[] {3, "x"}, contents.getArgs());
  }

  @Test
  void makeComponentWithoutArgsHasNoArgs() {
    TranslatableContents contents = assertInstanceOf(TranslatableContents.class, Mantle.makeComponent("gui", "title").getContents());
    assertEquals("gui.mantle.title", contents.getKey());
    assertEquals(0, contents.getArgs().length);
  }
}
