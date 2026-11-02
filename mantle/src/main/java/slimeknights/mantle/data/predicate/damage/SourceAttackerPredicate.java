package slimeknights.mantle.data.predicate.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;

/**
 * Predicate matching the entity behind a damage source.
 * A non-living entity, such as an arrow, matches only the "any" predicate.
 * @param attacker  Predicate for the entity
 * @param which     Whether to check the causing entity (the shooter) or the direct one (the arrow)
 */
public record SourceAttackerPredicate(IJsonPredicate<LivingEntity> attacker, WhichEntity which) implements DamageSourcePredicate {
  public static final RecordLoadable<SourceAttackerPredicate> LOADER = RecordLoadable.create(
    LivingEntityPredicate.LOADER.directField("entity_type", SourceAttackerPredicate::attacker),
    new EnumLoadable<>(WhichEntity.class).defaultField("which", WhichEntity.CAUSING, true, SourceAttackerPredicate::which),
    SourceAttackerPredicate::new);

  /** Creates a predicate checking the causing entity */
  public static SourceAttackerPredicate causing(IJsonPredicate<LivingEntity> attacker) {
    return new SourceAttackerPredicate(attacker, WhichEntity.CAUSING);
  }

  /** Creates a predicate checking the direct entity */
  public static SourceAttackerPredicate direct(IJsonPredicate<LivingEntity> attacker) {
    return new SourceAttackerPredicate(attacker, WhichEntity.DIRECT);
  }

  @Override
  public boolean matches(DamageSource source) {
    Entity entity = which.get(source);
    if (entity instanceof LivingEntity living) {
      return attacker.matches(living);
    }
    return attacker == LivingEntityPredicate.ANY && entity != null;
  }

  @Override
  public RecordLoadable<SourceAttackerPredicate> getLoader() {
    return LOADER;
  }

  /** Which entity of a damage source to check */
  public enum WhichEntity {
    /** The entity responsible, such as the shooter */
    CAUSING {
      @Override
      public @Nullable Entity get(DamageSource source) {
        return source.getEntity();
      }
    },
    /** The entity that hit, such as the arrow */
    DIRECT {
      @Override
      public @Nullable Entity get(DamageSource source) {
        return source.getDirectEntity();
      }
    };

    /** Gets the entity, or null when the source has none */
    public abstract @Nullable Entity get(DamageSource source);
  }
}
