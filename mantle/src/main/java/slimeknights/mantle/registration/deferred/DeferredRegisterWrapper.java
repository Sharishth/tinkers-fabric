package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Base logic for a deferred register wrapper
 * @param <T>  Registry type
 */
@SuppressWarnings("WeakerAccess")
public abstract class DeferredRegisterWrapper<T> {
  /** Registry instance, use this to provide register methods */
  protected final SynchronizedDeferredRegister<T> register;
  /** Mod ID for registration */
  protected final String modID;

  protected DeferredRegisterWrapper(ResourceKey<? extends Registry<T>> reg, String modID) {
    this(SynchronizedDeferredRegister.create(reg, modID), modID);
  }

  protected DeferredRegisterWrapper(SynchronizedDeferredRegister<T> register, String modID) {
    this.register = register;
    this.modID = modID;
  }

  /**
   * Queues this register for {@link RegistrationQueue#flush()}. Call during mod initialization.
   * Fabric replacement for upstream's register(IEventBus).
   */
  public void register() {
    register.register();
  }

  /* Utilities */

  /**
   * Gets an identifier for the given name
   * @param name  Name
   * @return  Identifier in this register's namespace
   */
  protected Identifier resource(String name) {
    return Identifier.fromNamespaceAndPath(modID, name);
  }

  /**
   * Gets a resource location string for the given name
   * @param name  Name
   * @return  Resource location string
   */
  protected String resourceName(String name) {
    return modID + ":" + name;
  }

  /**
   * Gets a registry key in this register's namespace
   * @param registry  Registry the key belongs to
   * @param name      Entry name
   * @return  Registry key
   */
  protected <K> ResourceKey<K> key(ResourceKey<? extends Registry<K>> registry, String name) {
    return ResourceKey.create(registry, resource(name));
  }


  /* Enum objects */

  // WHY buildEnumObject, not upstream's registerEnum: subclasses' public registerEnum(values, name, BiFunction) would clash after erasure

  /** Gets the name of an enum value */
  protected static String getName(Enum<?> value) {
    return value instanceof StringRepresentable representable ? representable.getSerializedName() : value.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Registers an item with multiple variants, prefixing the name with the value name
   * @param values    Enum values to use for this block
   * @param name      Name of the block
   * @param register  Function to register an entry
   * @return  EnumObject mapping between different block types
   */
  protected static <E extends Enum<E>, V extends T, T> EnumObject<E,V> buildEnumObject(E[] values, String name, BiFunction<String,E,Supplier<? extends V>> register) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least one value");
    }
    // note this cast only works because you cannot extend an enum
    EnumObject.Builder<E,V> builder = new EnumObject.Builder<>(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, register.apply(getName(value) + "_" + name, value));
    }
    return builder.build();
  }

  /**
   * Registers an item with multiple variants, suffixing the name with the value name
   * @param name      Name of the block
   * @param values    Enum values to use for this block
   * @param register  Function to register an entry
   * @return  EnumObject mapping between different block types
   */
  protected static <E extends Enum<E>, V extends T, T> EnumObject<E,V> buildEnumObject(String name, E[] values, BiFunction<String,E,Supplier<? extends V>> register) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least one value");
    }
    // note this cast only works because you cannot extend an enum
    EnumObject.Builder<E,V> builder = new EnumObject.Builder<>(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, register.apply(name + "_" + getName(value), value));
    }
    return builder.build();
  }
}
