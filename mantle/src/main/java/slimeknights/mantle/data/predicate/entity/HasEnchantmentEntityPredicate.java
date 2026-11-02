package slimeknights.mantle.data.predicate.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Predicate matching entities wearing or holding the enchantment.
 * Holds a holder since 26.x enchantments are a datapack registry (D-21).
 * @param enchantment  Enchantment to look for
 */
public record HasEnchantmentEntityPredicate(Holder<Enchantment> enchantment) implements LivingEntityPredicate {
  public static final RecordLoadable<HasEnchantmentEntityPredicate> LOADER = RecordLoadable.create(
    Loadables.ENCHANTMENT.requiredField("enchantment", HasEnchantmentEntityPredicate::enchantment), HasEnchantmentEntityPredicate::new);

  @Override
  public boolean matches(LivingEntity entity) {
    return EnchantmentHelper.getEnchantmentLevel(enchantment, entity) > 0;
  }

  @Override
  public RecordLoadable<HasEnchantmentEntityPredicate> getLoader() {
    return LOADER;
  }
}
