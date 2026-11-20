package slimeknights.mantle.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.JsonHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

/**
 * Data provider base that writes objects as JSON.
 * Works with a Fabric data generator as is: {@code FabricDataOutput} is a {@link PackOutput}.
 */
@RequiredArgsConstructor
public abstract class GenericDataProvider implements DataProvider {
  protected final PackOutput.PathProvider pathProvider;
  private final Gson gson;

  public GenericDataProvider(PackOutput output, Target type, String folder, Gson gson) {
    this(output.createPathProvider(type, folder), gson);
  }

  public GenericDataProvider(PackOutput output, Target type, String folder) {
    this(output, type, folder, JsonHelper.DEFAULT_GSON);
  }

  /**
   * Saves an object as JSON, logging instead of failing the run.
   * @param output         Output for writing
   * @param location       Location relative to this provider's folder
   * @param object         Object to save, converted with this provider's Gson
   * @param keyComparator  Order of object keys; null keeps the order they were added in
   */
  protected CompletableFuture<?> saveJson(CachedOutput output, Identifier location, Object object, @Nullable Comparator<String> keyComparator) {
    return saveStable(output, gson.toJsonTree(object), this.pathProvider.json(location), keyComparator).exceptionally(e -> {
      Mantle.logger.error("Couldn't create data for {}", location, e);
      return null;
    });
  }

  /** Saves an object as JSON with vanilla's key order */
  protected CompletableFuture<?> saveJson(CachedOutput output, Identifier location, Object object) {
    return saveJson(output, location, object, DataProvider.KEY_COMPARATOR);
  }

  /**
   * Saves an object as JSON using a codec.
   * @throws IllegalStateException  If the codec cannot write the object
   */
  protected <T> CompletableFuture<?> saveJson(CachedOutput output, Identifier location, Codec<T> codec, T object) {
    return saveJson(output, location, JsonHelper.serialize(codec, object));
  }

  /** Combines a stream of futures into one */
  public static CompletableFuture<?> allOf(Stream<CompletableFuture<?>> stream) {
    return CompletableFuture.allOf(stream.toArray(CompletableFuture[]::new));
  }

  /** Combines a list of futures into one */
  public static CompletableFuture<?> allOf(Collection<CompletableFuture<?>> tasks) {
    return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
  }

  /**
   * Writes stable JSON with a chosen key order.
   * Copy of {@link DataProvider#saveStable(CachedOutput, JsonElement, Path)}, which always sorts with vanilla's order.
   * WHY the suppression: the data generator cache compares SHA-1 hashes, the same deprecated Guava call vanilla makes.
   */
  @SuppressWarnings("deprecation")
  static CompletableFuture<?> saveStable(CachedOutput cache, JsonElement json, Path path, @Nullable Comparator<String> keyComparator) {
    return CompletableFuture.runAsync(() -> {
      try {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        HashingOutputStream hashingOutput = new HashingOutputStream(Hashing.sha1(), byteOutput);
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(hashingOutput, StandardCharsets.UTF_8))) {
          writer.setSerializeNulls(false);
          writer.setIndent("  ");
          GsonHelper.writeValue(writer, json, keyComparator);
        }
        cache.writeIfNeeded(path, byteOutput.toByteArray(), hashingOutput.hash());
      } catch (IOException exception) {
        throw new CompletionException(exception);
      }
    }, Util.backgroundExecutor());
  }
}
