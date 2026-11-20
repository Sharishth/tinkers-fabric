package slimeknights.mantle.gametest.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.datamap.BlockStateDataMapLoader;
import slimeknights.mantle.data.datamap.RegistryDataMapLoader;
import slimeknights.mantle.data.listener.MergingJsonDataLoader;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.gametest.registration.RegistrationTestContent;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reload listeners used by {@link ReloadListenerGameTest}.
 * Every value holds an enchantment, a datapack registry entry that only parses when the listener put the world
 * registries into the parse context (D-21).
 */
public class ReloadTestContent implements ModInitializer {
  /** Id of this test mod, from its fabric.mod.json */
  private static final String MOD_ID = "mantle-gametest";

  /** Data that needs world registries twice: an enchantment holder and a tag ingredient */
  public record EnchantedInput(Holder<Enchantment> enchantment, Ingredient input, int level) {
    static final RecordLoadable<EnchantedInput> LOADABLE = RecordLoadable.create(
      Loadables.ENCHANTMENT.requiredField("enchantment", EnchantedInput::enchantment),
      IngredientLoadable.DISALLOW_EMPTY.requiredField("input", EnchantedInput::input),
      IntLoadable.FROM_ONE.defaultField("level", 1, EnchantedInput::level),
      EnchantedInput::new);
  }

  public static final RegistryDataMapLoader<Item, EnchantedInput> ITEM_DATA = new RegistryDataMapLoader<>(
    "Test item data", PackType.SERVER_DATA, "mantle_reload_test/item_data", BuiltInRegistries.ITEM, EnchantedInput.LOADABLE);
  public static final BlockStateDataMapLoader<EnchantedInput> STATE_DATA = new BlockStateDataMapLoader<>(
    "Test state data", PackType.SERVER_DATA, "mantle_reload_test/state_data", EnchantedInput.LOADABLE);
  public static final MergedLoader MERGED = new MergedLoader();

  @Override
  public void onInitialize() {
    ResourceLoader loader = ResourceLoader.get(PackType.SERVER_DATA);
    loader.registerReloadListener(id("item_data"), ITEM_DATA);
    loader.registerReloadListener(id("state_data"), STATE_DATA);
    loader.registerReloadListener(id("merged"), MERGED);

    // a second pack holding the same merged file, so the merging loader has two copies to merge
    ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
    if (!ResourceLoader.registerBuiltinPack(id("merge_pack"), mod, PackActivationType.ALWAYS_ENABLED)) {
      throw new IllegalStateException("Test pack resourcepacks/merge_pack is missing from " + MOD_ID);
    }
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(RegistrationTestContent.NAMESPACE, path);
  }

  /** Values gathered from every pack's copy of one file */
  public static final class MergedEntry {
    public final List<String> values = new ArrayList<>();
    public @Nullable Holder<Enchantment> enchantment;
  }

  /** Merging loader that records what it was given, for the tests to inspect */
  public static final class MergedLoader extends MergingJsonDataLoader<MergedEntry> {
    private Map<Identifier, MergedEntry> entries = Map.of();
    private int loadCount = 0;
    private boolean hadRegistries = false;

    private MergedLoader() {
      super(PackType.SERVER_DATA, "mantle_reload_test/merged", id -> new MergedEntry());
    }

    @Override
    protected void parse(MergedEntry builder, Identifier id, JsonElement element, TypedMap context) {
      JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
      builder.values.add(GsonHelper.getAsString(json, "value"));
      if (json.has("enchantment")) {
        builder.enchantment = Loadables.ENCHANTMENT.getIfPresent(json, "enchantment", context);
      }
    }

    @Override
    protected void finishLoad(Map<Identifier, MergedEntry> map, ResourceManager manager, TypedMap context) {
      entries = map;
      hadRegistries = context.get(ContextKey.REGISTRIES) != null;
      loadCount++;
    }

    /** Gets the merged entry of a file, or null when no pack has it */
    public @Nullable MergedEntry get(Identifier id) {
      return entries.get(id);
    }

    /** Number of finished loads, 1 after the server started */
    public int loadCount() {
      return loadCount;
    }

    /** Whether the last load's context held registries */
    public boolean hadRegistries() {
      return hadRegistries;
    }
  }
}
