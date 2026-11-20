package slimeknights.mantle.data.datamap;

import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Data provider for {@link RegistryDataMapLoader}.
 * @param <R>  Registry entry type
 * @param <D>  Data type
 */
public abstract class RegistryDataMapProvider<R, D> extends GenericDataProvider {
  private final Registry<R> registry;
  private final RecordLoadable<D> dataLoader;
  private final String modId;
  private final Map<Identifier, Supplier<JsonObject>> entries = new HashMap<>();

  /**
   * Creates a provider from the parts of a loader.
   * WHY 6 parameters: upstream signature; {@link #RegistryDataMapProvider(PackOutput, Target, RegistryDataMapLoader, String)} is the short form.
   */
  public RegistryDataMapProvider(PackOutput output, Target type, Registry<R> registry, RecordLoadable<D> dataLoader, String folder, String modId) {
    super(output, type, folder);
    this.registry = registry;
    this.dataLoader = dataLoader;
    this.modId = modId;
  }

  /**
   * Creates a provider writing the files a loader reads.
   * @param type   Pack target; must match the pack type the loader is registered for
   * @param modId  Namespace of entries added by name
   */
  public RegistryDataMapProvider(PackOutput output, Target type, RegistryDataMapLoader<R, D> dataLoader, String modId) {
    this(output, type, dataLoader.getRegistry(), dataLoader.getDataLoader(), dataLoader.getFolder(), modId);
  }

  /** Adds all entries to this provider */
  protected abstract void addEntries();

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    addEntries();
    return allOf(entries.entrySet().stream().map(entry -> saveJson(cache, entry.getKey(), entry.getValue().get())));
  }


  /* Keys */

  /** Makes a file id in this provider's namespace */
  protected Identifier key(String name) {
    return Identifier.fromNamespaceAndPath(modId, name);
  }

  /** Gets the file id of a registry entry */
  protected Identifier key(R entry) {
    return Objects.requireNonNull(registry.getKey(entry), () -> "Unregistered entry " + entry);
  }

  /** Gets the file id of a registry entry */
  protected Identifier key(Supplier<? extends R> entry) {
    return key(entry.get());
  }


  /* Raw entries */

  /**
   * Adds a file to the provider.
   * @throws IllegalArgumentException  If the file was already added
   */
  protected void entry(Identifier key, Supplier<JsonObject> json) {
    Supplier<JsonObject> original = entries.putIfAbsent(key, json);
    if (original != null) {
      throw new IllegalArgumentException("Duplicate entry at " + key + ", original " + original + ", new value " + json);
    }
  }


  /* Redirects */

  /** Adds a file that uses another file's data */
  protected void redirect(Identifier key, Identifier parent) {
    entry(key, () -> {
      JsonObject json = new JsonObject();
      json.addProperty("parent", parent.toString());
      return json;
    });
  }

  /** Adds a file that uses another file's data */
  protected void redirect(String key, Identifier parent) {
    redirect(key(key), parent);
  }

  /** Makes a registry entry use another file's data */
  protected void redirect(R key, Identifier parent) {
    redirect(key(key), parent);
  }

  /** Makes a registry entry use another file's data */
  protected void redirect(Supplier<? extends R> key, Identifier parent) {
    redirect(key(key), parent);
  }

  /** Adds a file that uses another file's data */
  protected void redirect(String key, String parent) {
    redirect(key(key), key(parent));
  }

  /** Makes a registry entry use another file's data */
  protected void redirect(R key, String parent) {
    redirect(key(key), key(parent));
  }

  /** Makes a registry entry use another file's data */
  protected void redirect(Supplier<? extends R> key, String parent) {
    redirect(key(key), key(parent));
  }


  /* Data entries */

  /** Adds a file holding the data */
  protected void entry(Identifier key, D data) {
    entry(key, new DataEntry<>(dataLoader, data));
  }

  /** Adds a file holding the data, for other files to redirect to */
  protected void entry(String key, D data) {
    entry(key(key), data);
  }

  /** Sets the data of a registry entry */
  protected void entry(R key, D data) {
    entry(key(key), data);
  }

  /** Sets the data of a registry entry */
  protected void entry(Supplier<? extends R> key, D data) {
    entry(key(key), data);
  }

  /** Entry that writes its data when the provider runs */
  private record DataEntry<D>(RecordLoadable<D> loadable, D data) implements Supplier<JsonObject> {
    @Override
    public JsonObject get() {
      JsonObject json = new JsonObject();
      loadable.serialize(data, json);
      return json;
    }

    @Override
    public String toString() {
      return String.valueOf(data);
    }
  }
}
