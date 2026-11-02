package slimeknights.mantle.registration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks DelayedSupplier fails clearly until filled. */
class DelayedSupplierTest {
  @Test
  void getBeforeSetThrows() {
    DelayedSupplier<String> supplier = new DelayedSupplier<>();
    assertThrows(IllegalStateException.class, supplier::get);
  }

  @Test
  void getAfterSetDelegates() {
    DelayedSupplier<String> supplier = new DelayedSupplier<>();
    supplier.setSupplier(() -> "value");
    assertEquals("value", supplier.get());
  }
}
