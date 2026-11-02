package slimeknights.mantle.registration.deferred;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Deferred register for menu types.
 * Upstream wrapped Forge's IContainerFactory (reads a raw buffer); on Fabric, menus that need extra
 * data from the server use {@link ExtendedMenuType} with a {@link StreamCodec} for that data.
 */
@SuppressWarnings("unused")
public class MenuTypeDeferredRegister extends DeferredRegisterWrapper<MenuType<?>> {

  public MenuTypeDeferredRegister(String modID) {
    super(Registries.MENU, modID);
  }

  /**
   * Registers a menu type that needs no extra data to open
   * @param name     Menu name
   * @param factory  Menu factory
   * @param <C>      Menu type
   * @return  Registry object containing the menu type
   */
  public <C extends AbstractContainerMenu> RegistryObject<MenuType<C>> register(String name, MenuType.MenuSupplier<C> factory) {
    // MenuType's constructor is opened by Fabric API's transitive access widener
    return register.register(name, () -> new MenuType<>(factory, FeatureFlags.VANILLA_SET));
  }

  /**
   * Registers a menu type that receives extra data from the server, such as a block position
   * @param name     Menu name
   * @param factory  Menu factory, given the decoded data
   * @param codec    Encodes the data sent when opening the menu
   * @param <C>      Menu type
   * @param <D>      Data type
   * @return  Registry object containing the menu type
   */
  public <C extends AbstractContainerMenu, D> RegistryObject<ExtendedMenuType<C, D>> register(String name, ExtendedMenuType.ExtendedFactory<C, D> factory,
                                                                                              StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
    return register.register(name, () -> new ExtendedMenuType<>(factory, codec));
  }
}
