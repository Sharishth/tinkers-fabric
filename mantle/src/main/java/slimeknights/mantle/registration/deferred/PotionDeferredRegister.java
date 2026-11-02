package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Helper for registering potions.
 * 26.x effect instances take a {@code Holder<MobEffect>}, so effects are passed as holder suppliers.
 * Brewing recipes are data-driven in 26.3 and are not part of this register.
 */
@SuppressWarnings("unused")  // API
public class PotionDeferredRegister extends DeferredRegisterWrapper<Potion> {
  /** Upstream default: 3 minutes */
  private static final int DEFAULT_DURATION = 3 * 60 * 20;

  public PotionDeferredRegister(String modID) {
    super(Registries.POTION, modID);
  }

  /** Registers a standalone potion */
  public RegistryObject<Potion> register(String name, Supplier<Potion> potion) {
    return register.register(name, potion);
  }

  /** Registers a group of potions with the same effect */
  public Builder registerTypes(String name, Supplier<Holder<MobEffect>> effect, int duration, int amplifier) {
    return new Builder(name, effect, duration, amplifier);
  }

  /** Registers a group of potions with the same effect, named after the effect */
  public Builder registerTypes(RegistryObject<? extends MobEffect> effect, int duration, int amplifier) {
    return new Builder(effect.getId().getPath(), () -> asEffectHolder(effect), duration, amplifier);
  }

  /** Registers a group of potions with the same effect starting at level 1 and a duration of 3 minutes */
  public Builder registerTypes(RegistryObject<? extends MobEffect> effect) {
    return registerTypes(effect, DEFAULT_DURATION, 0);
  }

  /**
   * Widens a registered effect's holder to the registry type.
   * WHY the cast is safe: the holder belongs to the MobEffect registry; the subtype only narrows the Java type.
   */
  @SuppressWarnings("unchecked")
  private static Holder<MobEffect> asEffectHolder(RegistryObject<? extends MobEffect> effect) {
    return (Holder<MobEffect>) (Holder<?>) effect.getHolder()
      .orElseThrow(() -> new IllegalStateException("Effect " + effect.getId() + " is not registered yet; register effects before potions"));
  }

  /** Enum of potion variants for the builder */
  public enum PotionType {
    NORMAL,
    LONG,
    STRONG
  }

  /** Builder to create potion variants */
  public class Builder {
    private final EnumObject.Builder<PotionType,Potion> builder;
    private final String name;
    private final Supplier<Holder<MobEffect>> effect;
    private final int duration;
    private final int amplifier;

    private Builder(String name, Supplier<Holder<MobEffect>> effect, int duration, int amplifier) {
      this.builder = new EnumObject.Builder<>(PotionType.class);
      this.name = name;
      this.effect = effect;
      this.duration = duration;
      this.amplifier = amplifier;
      with(PotionType.NORMAL, duration, amplifier);
    }

    /**
     * Adds the given potion type.
     * Naming follows upstream's planned 1.21 change: normal is {@code name}, others {@code long_name} and {@code strong_name}.
     */
    private Builder with(PotionType type, int duration, int amplifier) {
      String id = type == PotionType.NORMAL ? name : type.toString().toLowerCase(Locale.ROOT) + '_' + name;
      builder.put(type, register(id, () -> new Potion(modID + "." + name, new MobEffectInstance(effect.get(), duration, amplifier))));
      return this;
    }

    /** Adds a strong potion with the given properties */
    public Builder withStrong(int duration, int amplifier) {
      return with(PotionType.STRONG, duration, amplifier);
    }

    /** Adds a strong potion with +1 to the level and half the duration */
    public Builder withStrong() {
      return withStrong(duration / 2, amplifier + 1);
    }

    /** Adds a long potion with the given properties */
    public Builder withLong(int duration, int amplifier) {
      return with(PotionType.LONG, duration, amplifier);
    }

    /** Adds a long potion with 8/3 the duration and same level */
    public Builder withLong() {
      return withLong(duration * 8 / 3, amplifier);
    }

    /** Builds the final object */
    public EnumObject<PotionType,Potion> build() {
      return builder.build();
    }
  }
}
