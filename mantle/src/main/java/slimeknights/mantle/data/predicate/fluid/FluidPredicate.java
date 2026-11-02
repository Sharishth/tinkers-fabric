package slimeknights.mantle.data.predicate.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.RegistryPredicateRegistry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Predicate over fluids */
public interface FluidPredicate extends IJsonPredicate<Fluid> {
  /** Matches every fluid */
  FluidPredicate ANY = simple(fluid -> true);
  /** Matches no fluid */
  FluidPredicate NONE = simple(fluid -> false);
  /** Registry of fluid predicate types */
  RegistryPredicateRegistry<Fluid, Fluid> LOADER = new RegistryPredicateRegistry<>(
    "Fluid Predicate", ANY, NONE, Loadables.FLUID, Function.identity(), "fluids", Loadables.FLUID_TAG,
    (tag, fluid) -> BuiltInRegistries.FLUID.wrapAsHolder(fluid).is(tag));

  /* Singletons */
  FluidPredicate SOURCE = simple(fluid -> fluid.isSource(fluid.defaultFluidState()));
  FluidPredicate HAS_BUCKET = simple(fluid -> fluid.getBucket() != Items.AIR);
  /** WHY Fabric attributes: upstream asked Forge's FluidType; FluidVariant only accepts still fluids, so flowing ones use their source */
  FluidPredicate LIGHTER_THAN_AIR = simple(fluid -> FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid)));

  @Override
  default IJsonPredicate<Fluid> inverted() {
    return LOADER.invert(this);
  }

  /** Creates a fieldless predicate with its own loader */
  static FluidPredicate simple(Predicate<Fluid> predicate) {
    return SingletonLoader.singleton(loader -> new FluidPredicate() {
      @Override
      public boolean matches(Fluid fluid) {
        return predicate.test(fluid);
      }

      @Override
      public RecordLoadable<? extends FluidPredicate> getLoader() {
        return loader;
      }
    });
  }

  /** Creates a predicate matching any of the fluids */
  static IJsonPredicate<Fluid> set(Fluid... fluids) {
    return LOADER.setOf(fluids);
  }

  /** Creates a predicate matching fluids in the tag */
  static IJsonPredicate<Fluid> tag(TagKey<Fluid> tag) {
    return LOADER.tag(tag);
  }

  /** Creates a predicate matching when all match */
  @SafeVarargs
  static IJsonPredicate<Fluid> and(IJsonPredicate<Fluid>... predicates) {
    return LOADER.and(List.of(predicates));
  }

  /** Creates a predicate matching when any matches */
  @SafeVarargs
  static IJsonPredicate<Fluid> or(IJsonPredicate<Fluid>... predicates) {
    return LOADER.or(List.of(predicates));
  }
}
