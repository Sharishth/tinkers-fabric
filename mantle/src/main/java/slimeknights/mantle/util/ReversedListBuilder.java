package slimeknights.mantle.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collects elements and hands them out last first.
 * Used when merging data from packs that arrive highest priority first but must be applied lowest first.
 * @param <E>  Element type
 */
public class ReversedListBuilder<E> {
  private final List<E> unpacked = new ArrayList<>();

  /** Adds an element */
  public void add(E data) {
    unpacked.add(data);
  }

  /** Gets the number of elements */
  public int size() {
    return unpacked.size();
  }

  /** Gives each element to the consumer, last added first */
  public void build(Consumer<E> consumer) {
    for (int i = unpacked.size() - 1; i >= 0; i--) {
      consumer.accept(unpacked.get(i));
    }
  }

  /** Gets the total size of all collections in the builder */
  public static int size(ReversedListBuilder<? extends Collection<?>> builder) {
    return builder.unpacked.stream().mapToInt(Collection::size).sum();
  }

  /** Joins the collections into one immutable list, last added collection first */
  public static <E> List<E> buildList(ReversedListBuilder<? extends Collection<E>> builder) {
    int size = size(builder);
    if (size == 0) {
      return List.of();
    }
    List<E> list = new ArrayList<>(size);
    builder.build(list::addAll);
    return List.copyOf(list);
  }
}
