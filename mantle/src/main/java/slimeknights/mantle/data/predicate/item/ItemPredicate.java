package slimeknights.mantle.data.predicate.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.RegistryPredicateRegistry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Predicate over items */
public interface ItemPredicate extends IJsonPredicate<Item> {
  /** Matches every item */
  ItemPredicate ANY = simple(item -> true);
  /** Matches no item */
  ItemPredicate NONE = simple(item -> false);
  /** Registry of item predicate types */
  RegistryPredicateRegistry<Item, Item> LOADER = new RegistryPredicateRegistry<>(
    "Item Predicate", ANY, NONE, Loadables.ITEM, Function.identity(), "items", Loadables.ITEM_TAG,
    (tag, item) -> BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag));

  /** Items that leave something behind in crafting, such as buckets */
  ItemPredicate HAS_CONTAINER = simple(item -> item.getCraftingRemainder() != null);
  // TODO(T-50): MAY_HAVE_TRANSFER (upstream FluidContainerTransferManager.mayHaveTransfer), registered as mantle:may_have_transfer

  @Override
  default IJsonPredicate<Item> inverted() {
    return LOADER.invert(this);
  }

  /** Creates a fieldless predicate with its own loader */
  static ItemPredicate simple(Predicate<Item> predicate) {
    return SingletonLoader.singleton(loader -> new ItemPredicate() {
      @Override
      public boolean matches(Item item) {
        return predicate.test(item);
      }

      @Override
      public RecordLoadable<? extends ItemPredicate> getLoader() {
        return loader;
      }
    });
  }

  /** Creates a predicate matching any of the items */
  static IJsonPredicate<Item> set(Item... items) {
    return LOADER.setOf(items);
  }

  /** Creates a predicate matching items in the tag */
  static IJsonPredicate<Item> tag(TagKey<Item> tag) {
    return LOADER.tag(tag);
  }

  /** Creates a predicate matching when all match */
  @SafeVarargs
  static IJsonPredicate<Item> and(IJsonPredicate<Item>... predicates) {
    return LOADER.and(List.of(predicates));
  }

  /** Creates a predicate matching when any matches */
  @SafeVarargs
  static IJsonPredicate<Item> or(IJsonPredicate<Item>... predicates) {
    return LOADER.or(List.of(predicates));
  }
}
