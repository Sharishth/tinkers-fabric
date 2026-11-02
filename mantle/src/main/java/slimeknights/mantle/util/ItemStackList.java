package slimeknights.mantle.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** List of item stacks where missing stacks are {@link ItemStack#EMPTY}, with inventory-style helpers */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ItemStackList extends NonNullList<ItemStack> {
  protected ItemStackList() {
    this(new ArrayList<>());
  }

  protected ItemStackList(List<ItemStack> delegate) {
    super(delegate, ItemStack.EMPTY);
  }

  /** Creates an empty, growable list */
  @SuppressWarnings("unchecked")  // hides NonNullList's generic create(); always ItemStack here
  public static ItemStackList create() {
    return new ItemStackList();
  }

  /** Creates a fixed-size list filled with empty stacks */
  public static ItemStackList withSize(int size) {
    ItemStack[] stacks = new ItemStack[size];
    Arrays.fill(stacks, ItemStack.EMPTY);
    return new ItemStackList(Arrays.asList(stacks));
  }

  /** Creates a growable list of the stacks */
  public static ItemStackList of(ItemStack... stacks) {
    return of(Arrays.asList(stacks));
  }

  /** Creates a growable list of the stacks */
  public static ItemStackList of(Collection<ItemStack> stacks) {
    ItemStackList list = create();
    list.addAll(stacks);
    return list;
  }

  /**
   * Creates a fixed-size list of the container's stacks, not copied.
   * WHY set: upstream called add on the fixed-size list, which always threw.
   */
  public static ItemStackList of(Container inventory) {
    ItemStackList list = withSize(inventory.getContainerSize());
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      list.set(i, inventory.getItem(i));
    }
    return list;
  }

  /** Checks if the index holds a non-empty stack */
  public boolean hasItem(int index) {
    return index >= 0 && index < size() && !get(index).isEmpty();
  }

  /** Empties the index if it exists */
  public void setEmpty(int index) {
    if (index >= 0 && index < size()) {
      set(index, ItemStack.EMPTY);
    }
  }

  /**
   * Copies the list, sharing the stacks.
   * @param fixed  If true the copy is fixed-size, otherwise growable
   */
  public ItemStackList copy(boolean fixed) {
    return copyWith(fixed, false);
  }

  /**
   * Copies the list and each stack.
   * @param fixed  If true the copy is fixed-size, otherwise growable
   */
  public ItemStackList deepCopy(boolean fixed) {
    return copyWith(fixed, true);
  }

  /** WHY two paths: upstream called set on the empty growable copy, which threw for any non-empty list */
  private ItemStackList copyWith(boolean fixed, boolean copyStacks) {
    ItemStackList copy = fixed ? withSize(size()) : create();
    for (int i = 0; i < size(); i++) {
      ItemStack stack = copyStacks ? get(i).copy() : get(i);
      if (fixed) {
        copy.set(i, stack);
      } else {
        copy.add(stack);
      }
    }
    return copy;
  }
}
