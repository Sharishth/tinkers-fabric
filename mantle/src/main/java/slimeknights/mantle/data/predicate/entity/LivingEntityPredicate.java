package slimeknights.mantle.data.predicate.entity;

import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.RegistryPredicateRegistry;

import java.util.List;
import java.util.function.Predicate;

/** Predicate over living entities */
public interface LivingEntityPredicate extends IJsonPredicate<LivingEntity> {
  /** Matches every entity */
  LivingEntityPredicate ANY = simple(entity -> true);
  /** Matches no entity */
  LivingEntityPredicate NONE = simple(entity -> false);
  /** Registry of entity predicate types */
  RegistryPredicateRegistry<EntityType<?>, LivingEntity> LOADER = new RegistryPredicateRegistry<>(
    "Entity Predicate", ANY, NONE, Loadables.ENTITY_TYPE, Entity::getType, "entities", Loadables.ENTITY_TYPE_TAG, (tag, entity) -> entity.typeHolder().is(tag));

  /* Singletons */
  LivingEntityPredicate WATER_SENSITIVE = simple(LivingEntity::isSensitiveToWater);
  LivingEntityPredicate FIRE_IMMUNE = simple(Entity::fireImmune);
  LivingEntityPredicate ON_FIRE = simple(Entity::isOnFire);
  LivingEntityPredicate CAN_FREEZE = simple(Entity::canFreeze);
  LivingEntityPredicate IS_FREEZING = simple(entity -> entity.getTicksFrozen() >= entity.getTicksRequiredToFreeze());
  LivingEntityPredicate IS_IN_POWDERED_SNOW = simple(Entity::isFreezing);
  LivingEntityPredicate ON_GROUND = simple(Entity::onGround);
  LivingEntityPredicate CROUCHING = simple(Entity::isCrouching);
  LivingEntityPredicate SPRINTING = simple(Entity::isSprinting);
  LivingEntityPredicate BLOCKING = simple(LivingEntity::isBlocking);
  LivingEntityPredicate ELYTRA_FLYING = simple(LivingEntity::isFallFlying);
  /** WHY isEyeInFluid: upstream read wasEyeInWater, now protected; vanilla sets it from this check each tick */
  LivingEntityPredicate EYES_IN_WATER = simple(entity -> entity.isEyeInFluid(FluidTags.WATER));
  LivingEntityPredicate FEET_IN_WATER = simple(Entity::isInWater);
  LivingEntityPredicate UNDERWATER = simple(Entity::isUnderWater);
  LivingEntityPredicate RAINING = simple(entity -> entity.level().isRainingAt(entity.blockPosition()));

  @Override
  default IJsonPredicate<LivingEntity> inverted() {
    return LOADER.invert(this);
  }

  /** Creates a fieldless predicate with its own loader */
  static LivingEntityPredicate simple(Predicate<LivingEntity> predicate) {
    return SingletonLoader.singleton(loader -> new LivingEntityPredicate() {
      @Override
      public boolean matches(LivingEntity entity) {
        return predicate.test(entity);
      }

      @Override
      public RecordLoadable<? extends LivingEntityPredicate> getLoader() {
        return loader;
      }
    });
  }

  /** Creates a predicate matching any of the entity types */
  static IJsonPredicate<LivingEntity> set(EntityType<?>... types) {
    return LOADER.setOf(types);
  }

  /** Creates a predicate matching entity types in the tag */
  static IJsonPredicate<LivingEntity> tag(TagKey<EntityType<?>> tag) {
    return LOADER.tag(tag);
  }

  /** Creates a predicate matching when all match */
  @SafeVarargs
  static IJsonPredicate<LivingEntity> and(IJsonPredicate<LivingEntity>... predicates) {
    return LOADER.and(List.of(predicates));
  }

  /** Creates a predicate matching when any matches */
  @SafeVarargs
  static IJsonPredicate<LivingEntity> or(IJsonPredicate<LivingEntity>... predicates) {
    return LOADER.or(List.of(predicates));
  }
}
