package slimeknights.mantle.data.predicate.damage;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.TagPredicateRegistry;

import java.util.List;
import java.util.function.Predicate;

/** Predicate over damage sources */
public interface DamageSourcePredicate extends IJsonPredicate<DamageSource> {
  /** Matches every source */
  DamageSourcePredicate ANY = simple(source -> true);
  /** Matches no source */
  DamageSourcePredicate NONE = simple(source -> false);
  /** Registry of damage source predicate types */
  TagPredicateRegistry<DamageType, DamageSource> LOADER = new TagPredicateRegistry<>(
    "Damage Source Predicate", ANY, NONE, Loadables.DAMAGE_TYPE_TAG, (tag, source) -> source.is(tag));

  /* Singletons */
  /** WHY !isDirect: 26.x replaced isIndirect with its opposite */
  DamageSourcePredicate IS_INDIRECT = simple(source -> !source.isDirect());
  DamageSourcePredicate HAS_ENTITY = simple(source -> source.getEntity() != null);
  /** Damage that armor and protection can reduce */
  DamageSourcePredicate CAN_PROTECT = simple(source -> !source.is(DamageTypeTags.BYPASSES_EFFECTS) && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
                                                       && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));

  @Override
  default IJsonPredicate<DamageSource> inverted() {
    return LOADER.invert(this);
  }

  /** Creates a fieldless predicate with its own loader */
  static DamageSourcePredicate simple(Predicate<DamageSource> predicate) {
    return SingletonLoader.singleton(loader -> new DamageSourcePredicate() {
      @Override
      public boolean matches(DamageSource source) {
        return predicate.test(source);
      }

      @Override
      public RecordLoadable<? extends DamageSourcePredicate> getLoader() {
        return loader;
      }
    });
  }

  /** Creates a predicate matching damage types in the tag */
  static IJsonPredicate<DamageSource> tag(TagKey<DamageType> tag) {
    return LOADER.tag(tag);
  }

  /** Creates a predicate matching when all match */
  @SafeVarargs
  static IJsonPredicate<DamageSource> and(IJsonPredicate<DamageSource>... predicates) {
    return LOADER.and(List.of(predicates));
  }

  /** Creates a predicate matching when any matches */
  @SafeVarargs
  static IJsonPredicate<DamageSource> or(IJsonPredicate<DamageSource>... predicates) {
    return LOADER.or(List.of(predicates));
  }
}
