package slimeknights.mantle.data.loadable;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import slimeknights.mantle.data.loadable.common.CodecLoadable;
import slimeknights.mantle.data.loadable.common.DynamicRegistryLoadable;
import slimeknights.mantle.data.loadable.common.RegistryLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.mantle.data.loadable.primitive.IdentifierLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable.IntNetwork;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import java.util.function.BiFunction;

/**
 * Shared loadables for vanilla types.
 * Not ported: TOOL_ACTION (Mantle ToolAction, T-37) and FLUID_TYPE (Forge fluid types; Fabric fluids at T-50).
 */
@SuppressWarnings("unused")  // API
public final class Loadables {
  private Loadables() {}

  /** Plain identifier; upstream's RESOURCE_LOCATION */
  public static final StringLoadable<Identifier> IDENTIFIER = IdentifierLoadable.DEFAULT;

  /* Built-in registries */
  public static final IdentifierLoadable<SoundEvent> SOUND_EVENT = new RegistryLoadable<>(BuiltInRegistries.SOUND_EVENT);
  public static final IdentifierLoadable<Fluid> FLUID = new RegistryLoadable<>(BuiltInRegistries.FLUID);
  public static final IdentifierLoadable<MobEffect> MOB_EFFECT = new RegistryLoadable<>(BuiltInRegistries.MOB_EFFECT);
  public static final IdentifierLoadable<Block> BLOCK = new RegistryLoadable<>(BuiltInRegistries.BLOCK);
  public static final IdentifierLoadable<EntityType<?>> ENTITY_TYPE = new RegistryLoadable<>(BuiltInRegistries.ENTITY_TYPE);
  public static final IdentifierLoadable<Item> ITEM = new RegistryLoadable<>(BuiltInRegistries.ITEM);
  public static final IdentifierLoadable<Potion> POTION = new RegistryLoadable<>(BuiltInRegistries.POTION);
  public static final IdentifierLoadable<ParticleType<?>> PARTICLE_TYPE = new RegistryLoadable<>(BuiltInRegistries.PARTICLE_TYPE);
  public static final IdentifierLoadable<BlockEntityType<?>> BLOCK_ENTITY_TYPE = new RegistryLoadable<>(BuiltInRegistries.BLOCK_ENTITY_TYPE);
  public static final IdentifierLoadable<Attribute> ATTRIBUTE = new RegistryLoadable<>(BuiltInRegistries.ATTRIBUTE);
  public static final IdentifierLoadable<RecipeType<?>> RECIPE_TYPE = new RegistryLoadable<>(BuiltInRegistries.RECIPE_TYPE);

  /* Datapack registries (D-21): holders, needing world registries in the parse context */
  /** Enchantment holder; upstream read Enchantment values from a static registry, which 26.x no longer has */
  public static final IdentifierLoadable<Holder<Enchantment>> ENCHANTMENT = new DynamicRegistryLoadable<>(Registries.ENCHANTMENT);

  /* Registries without their empty value */
  public static final StringLoadable<Fluid> NON_EMPTY_FLUID = notValue(FLUID, Fluids.EMPTY, "Fluid cannot be empty");
  public static final StringLoadable<Block> NON_EMPTY_BLOCK = notValue(BLOCK, Blocks.AIR, "Block cannot be air");
  public static final StringLoadable<Item> NON_EMPTY_ITEM = notValue(ITEM, Items.AIR, "Item cannot be empty");

  /* Tag keys */
  public static final StringLoadable<TagKey<Fluid>> FLUID_TAG = tagKey(Registries.FLUID);
  public static final StringLoadable<TagKey<MobEffect>> MOB_EFFECT_TAG = tagKey(Registries.MOB_EFFECT);
  public static final StringLoadable<TagKey<Block>> BLOCK_TAG = tagKey(Registries.BLOCK);
  public static final StringLoadable<TagKey<Enchantment>> ENCHANTMENT_TAG = tagKey(Registries.ENCHANTMENT);
  public static final StringLoadable<TagKey<EntityType<?>>> ENTITY_TYPE_TAG = tagKey(Registries.ENTITY_TYPE);
  public static final StringLoadable<TagKey<Item>> ITEM_TAG = tagKey(Registries.ITEM);
  public static final StringLoadable<TagKey<Potion>> POTION_TAG = tagKey(Registries.POTION);
  public static final StringLoadable<TagKey<BlockEntityType<?>>> BLOCK_ENTITY_TYPE_TAG = tagKey(Registries.BLOCK_ENTITY_TYPE);
  public static final StringLoadable<TagKey<DamageType>> DAMAGE_TYPE_TAG = tagKey(Registries.DAMAGE_TYPE);

  /* Resource keys */
  public static final StringLoadable<ResourceKey<DamageType>> DAMAGE_TYPE_KEY = resourceKey(Registries.DAMAGE_TYPE);
  public static final StringLoadable<ResourceKey<Enchantment>> ENCHANTMENT_KEY = resourceKey(Registries.ENCHANTMENT);

  /* Other */
  /** Loot pool entry in vanilla's 26.x format; upstream used Forge's loot modifier Gson */
  public static final Loadable<LootPoolEntryContainer> LOOT_ENTRY = new CodecLoadable<>(LootPoolEntries.CODEC);
  /** Model rotation in degrees; one of 0, 90, 180 or 270 */
  public static final Loadable<Integer> ROTATION = new IntLoadable(0, 270, IntNetwork.SHORT).validate((value, error) -> {
    // WHY inline: upstream called the client ModelHelper, which server code cannot load
    if (value % 90 != 0) {
      throw error.create("Rotation must be 0/90/180/270");
    }
    return value;
  });
  public static final EnumLoadable<Direction> DIRECTION = new EnumLoadable<>(Direction.class);

  /** Creates a loadable for tag keys of a registry */
  public static <T> StringLoadable<TagKey<T>> tagKey(ResourceKey<? extends Registry<T>> registry) {
    return IDENTIFIER.map(key -> TagKey.create(registry, key), TagKey::location);
  }

  /** Creates a loadable for resource keys of a registry */
  public static <T> StringLoadable<ResourceKey<T>> resourceKey(ResourceKey<? extends Registry<T>> registry) {
    return IDENTIFIER.map(key -> ResourceKey.create(registry, key), ResourceKey::identifier);
  }

  /** Makes a loadable that rejects one value, such as air */
  public static <T> StringLoadable<T> notValue(StringLoadable<T> loadable, T notValue, String errorMsg) {
    BiFunction<T, ErrorFactory, T> mapper = (value, error) -> {
      if (value == notValue) {
        throw error.create(errorMsg);
      }
      return value;
    };
    return loadable.mapChecked(mapper, mapper);
  }
}
