package slimeknights.mantle.gametest.data;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.util.typed.TypedMapBuilder;

/** Checks entity and damage predicates on real entities; the types are registered by Mantle's entrypoint */
public class PredicateGameTest {
  private static IJsonPredicate<LivingEntity> entity(GameTestHelper helper, String json) {
    var context = TypedMapBuilder.builder().put(ContextKey.REGISTRIES, helper.getLevel().registryAccess()).build();
    return LivingEntityPredicate.LOADER.convert(JsonParser.parseString(json), "predicate", context);
  }

  @GameTest
  public void mobGroupsUseEntityTypeTags(GameTestHelper helper) {
    LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
    LivingEntity cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 1, 1));
    IJsonPredicate<LivingEntity> undead = entity(helper, "{\"type\": \"mantle:mob_type\", \"mobs\": \"minecraft:undead\"}");
    IJsonPredicate<LivingEntity> undefined = entity(helper, "{\"type\": \"mantle:mob_type\", \"mobs\": \"minecraft:undefined\"}");
    helper.assertTrue(undead.matches(zombie) && !undead.matches(cow), "zombie is undead, cow is not");
    helper.assertTrue(undefined.matches(cow) && !undefined.matches(zombie), "cow is in no mob group");
    helper.assertTrue(entity(helper, "{\"type\": \"mantle:set\", \"entities\": [\"minecraft:cow\"]}").matches(cow), "entity type set");
    helper.succeed();
  }

  @GameTest
  public void effectBlockAndEnchantmentPredicates(GameTestHelper helper) {
    LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
    helper.setBlock(new BlockPos(1, 1, 1), Blocks.GOLD_BLOCK);
    zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, 200));
    helper.assertTrue(entity(helper, "{\"type\": \"mantle:has_effect\", \"effect\": \"minecraft:speed\"}").matches(zombie), "has speed");
    helper.assertFalse(entity(helper, "{\"type\": \"mantle:has_effect\", \"effect\": \"minecraft:haste\"}").matches(zombie), "no haste");
    IJsonPredicate<LivingEntity> onGold = entity(helper, "{\"type\": \"mantle:block_at_entity\", \"block_type\": \"mantle:set\", \"blocks\": [\"minecraft:gold_block\"], \"offset\": -1}");
    helper.assertTrue(onGold.matches(zombie), "standing on gold");
    helper.assertFalse(entity(helper, "{\"type\": \"mantle:has_enchantment\", \"enchantment\": \"minecraft:protection\"}").matches(zombie), "no protection");
    helper.succeed();
  }

  @GameTest
  public void damagePredicatesCheckTypeAndAttacker(GameTestHelper helper) {
    Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
    DamageSource attack = helper.getLevel().damageSources().mobAttack(zombie);
    DamageSource fall = helper.getLevel().damageSources().fall();
    IJsonPredicate<DamageSource> byUndead = DamageSourcePredicate.LOADER.convert(JsonParser.parseString(
      "{\"type\": \"mantle:attacker\", \"entity_type\": \"mantle:mob_type\", \"mobs\": \"minecraft:undead\"}"), "predicate");
    helper.assertTrue(byUndead.matches(attack) && !byUndead.matches(fall), "attacker predicate");
    IJsonPredicate<DamageSource> isFall = DamageSourcePredicate.LOADER.convert(JsonParser.parseString("{\"type\": \"mantle:damage_type\", \"name\": \"minecraft:fall\"}"), "predicate");
    helper.assertTrue(isFall.matches(fall) && !isFall.matches(attack), "damage type predicate");
    helper.assertTrue(DamageSourcePredicate.HAS_ENTITY.matches(attack) && !DamageSourcePredicate.HAS_ENTITY.matches(fall), "has entity");
    helper.assertTrue(DamageSourcePredicate.CAN_PROTECT.matches(attack), "armor protects against mob attacks");
    helper.succeed();
  }
}
