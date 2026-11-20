package slimeknights.mantle.data.datamap;

import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.data.loadable.Loadable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Data provider for {@link BlockStateDataMapLoader}.
 * @param <D>  Data type
 */
public abstract class BlockStateDataMapProvider<D> extends GenericDataProvider {
  private final Loadable<D> dataLoader;
  private final String modId;
  private final Map<Block, DataMap> blocks = new HashMap<>();
  private final Map<Identifier, D> entries = new HashMap<>();

  /**
   * Creates a provider from the parts of a loader.
   * @param modId  Namespace of entries added by name
   */
  public BlockStateDataMapProvider(PackOutput output, Target type, String folder, Loadable<D> dataLoader, String modId) {
    super(output, type, folder);
    this.dataLoader = dataLoader;
    this.modId = modId;
  }

  /**
   * Creates a provider writing the files a loader reads.
   * @param type   Pack target; must match the pack type the loader is registered for
   * @param modId  Namespace of entries added by name
   */
  public BlockStateDataMapProvider(PackOutput output, Target type, BlockStateDataMapLoader<D> registry, String modId) {
    this(output, type, registry.getFolder(), registry.getDataLoader(), modId);
  }

  /** Adds all entries to this provider */
  protected abstract void addEntries();

  @Override
  public CompletableFuture<?> run(CachedOutput cached) {
    addEntries();
    return allOf(Stream.concat(
      blocks.values().stream().map(entry -> saveJson(cached, BuiltInRegistries.BLOCK.getKey(entry.owner), entry.toJson())),
      entries.entrySet().stream().map(entry -> saveJson(cached, entry.getKey(), dataLoader.serialize(entry.getValue())))
    ));
  }

  /** Gets the builder of a block's file, creating it on first use */
  protected DataMap block(Block block) {
    return blocks.computeIfAbsent(block, DataMap::new);
  }

  /** Gets the builder of a block's file, creating it on first use */
  protected DataMap block(Supplier<? extends Block> block) {
    return block(block.get());
  }

  /**
   * Adds a shared file that block variants may redirect to.
   * @throws IllegalArgumentException  If the file was already added
   */
  protected void entry(Identifier key, D data) {
    D original = entries.putIfAbsent(key, data);
    if (original != null) {
      throw new IllegalArgumentException("Duplicate entry at " + key + ", original " + original + ", new value " + data);
    }
  }

  /** Adds a shared file in this provider's namespace */
  protected void entry(String key, D data) {
    entry(Identifier.fromNamespaceAndPath(modId, key), data);
  }

  /** One entry of a variants list: either inline data or the id of a shared file */
  private record Variant<D>(@Nullable D data, @Nullable Identifier parent, StateVariantStringBuilder variant) {}

  /** Builder of the file of one block */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  protected class DataMap {
    private final Block owner;
    private final List<Variant<D>> variants = new ArrayList<>();

    /** Adds a variant with its data written inline */
    public VariantBuilder variant(D data) {
      VariantBuilder builder = new VariantBuilder();
      variants.add(new Variant<>(data, null, builder));
      return builder;
    }

    /** Adds a variant using a shared file */
    public VariantBuilder variant(Identifier parent) {
      VariantBuilder builder = new VariantBuilder();
      variants.add(new Variant<>(null, parent, builder));
      return builder;
    }

    /** Adds a variant using a shared file in this provider's namespace */
    public VariantBuilder variant(String parent) {
      return variant(Identifier.fromNamespaceAndPath(modId, parent));
    }

    /** Serializes this file to JSON */
    private JsonObject toJson() {
      JsonObject variantsJson = new JsonObject();
      for (Variant<D> variant : this.variants) {
        String variantString = variant.variant.toString();
        if (variant.data != null) {
          variantsJson.add(variantString, dataLoader.serialize(variant.data));
        } else if (variant.parent != null) {
          variantsJson.addProperty(variantString, variant.parent.toString());
        }
      }
      JsonObject map = new JsonObject();
      map.add("variants", variantsJson);
      return map;
    }

    /** Variant key builder that returns to its file when finished */
    public class VariantBuilder extends StateVariantStringBuilder {
      protected VariantBuilder() {
        super(owner);
      }

      @Override
      public <T extends Comparable<T>> VariantBuilder when(Property<T> prop, T value) {
        super.when(prop, value);
        return this;
      }

      /** Returns to the file being built */
      public DataMap end() {
        return DataMap.this;
      }
    }
  }
}
