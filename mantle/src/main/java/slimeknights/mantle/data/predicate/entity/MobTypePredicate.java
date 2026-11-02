package slimeknights.mantle.data.predicate.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.NamedComponentRegistry;

import java.util.List;

/**
 * Predicate matching entities of a mob group, such as undead.
 * WHY tags: 26.x removed MobType and moved the groups to entity type tags; the JSON names stay upstream's.
 * @param type  Group to match
 */
public record MobTypePredicate(MobGroup type) implements LivingEntityPredicate {
  /** Groups by their JSON name, {@code minecraft:undead} and so on */
  public static final NamedComponentRegistry<MobGroup> MOB_TYPES = new NamedComponentRegistry<>("Unknown mob type");
  public static final RecordLoadable<MobTypePredicate> LOADER = RecordLoadable.create(MOB_TYPES.requiredField("mobs", MobTypePredicate::type), MobTypePredicate::new);

  @Override
  public boolean matches(LivingEntity input) {
    return type.matches(input);
  }

  @Override
  public RecordLoadable<? extends LivingEntityPredicate> getLoader() {
    return LOADER;
  }

  /** Registers every group under its upstream name */
  public static void registerGroups() {
    for (MobGroup group : MobGroup.values()) {
      MOB_TYPES.register(Identifier.withDefaultNamespace(group.jsonName), group);
    }
  }

  /** Mob groups from 1.20's MobType, now backed by entity type tags */
  public enum MobGroup {
    /** In none of the other groups */
    UNDEFINED("undefined", null),
    UNDEAD("undead", EntityTypeTags.UNDEAD),
    ARTHROPOD("arthropod", EntityTypeTags.ARTHROPOD),
    ILLAGER("illager", EntityTypeTags.ILLAGER),
    WATER("water", EntityTypeTags.AQUATIC);

    /** Tags of the defined groups, used by {@link #UNDEFINED} */
    private static final List<TagKey<EntityType<?>>> DEFINED = List.of(EntityTypeTags.UNDEAD, EntityTypeTags.ARTHROPOD, EntityTypeTags.ILLAGER, EntityTypeTags.AQUATIC);

    private final String jsonName;
    private final @Nullable TagKey<EntityType<?>> tag;

    MobGroup(String jsonName, @Nullable TagKey<EntityType<?>> tag) {
      this.jsonName = jsonName;
      this.tag = tag;
    }

    /** Checks if the entity's type is in this group */
    public boolean matches(LivingEntity entity) {
      if (tag != null) {
        return entity.typeHolder().is(tag);
      }
      return DEFINED.stream().noneMatch(entity.typeHolder()::is);
    }
  }
}
