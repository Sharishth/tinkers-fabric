package slimeknights.mantle.data.predicate.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;

/**
 * Predicate matching entities with the effect active.
 * @param effect  Effect to look for
 */
public record HasMobEffectPredicate(MobEffect effect) implements LivingEntityPredicate {
  public static final RecordLoadable<HasMobEffectPredicate> LOADER = RecordLoadable.create(
    Loadables.MOB_EFFECT.requiredField("effect", HasMobEffectPredicate::effect), HasMobEffectPredicate::new);

  // WHY wrapAsHolder: 26.x LivingEntity.hasEffect takes a holder
  @Override
  public boolean matches(LivingEntity living) {
    return living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
  }

  @Override
  public RecordLoadable<? extends IJsonPredicate<LivingEntity>> getLoader() {
    return LOADER;
  }
}
