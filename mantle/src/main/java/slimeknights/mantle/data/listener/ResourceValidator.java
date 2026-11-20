package slimeknights.mantle.data.listener;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Checks whether a resource exists in any pack, from a list made on reload */
@SuppressWarnings("unused")  // API
public class ResourceValidator implements IEarlyReloadListener, Predicate<Identifier> {
  private final String folder;
  private final int trim;
  private final String extension;
  /** Ids found on the last reload; volatile as the prepare phase writes it off the game thread */
  protected volatile Set<Identifier> resources;

  /**
   * Creates a validator.
   * @param folder     Folder to search
   * @param trim       Text to trim off the front of paths, such as {@code textures}; the slash after it is trimmed too
   * @param extension  File extension, with the dot
   */
  public ResourceValidator(String folder, String trim, String extension) {
    this.folder = folder;
    this.trim = trim.length() + 1;
    this.extension = extension;
    this.resources = Set.of();
  }

  @Override
  public void onResourceManagerReload(ResourceManager manager) {
    int extensionLength = extension.length();
    this.resources = manager.listResources(folder, location -> location.getPath().endsWith(extension)).keySet().stream()
      .map(location -> {
        String path = location.getPath();
        return location.withPath(path.substring(trim, path.length() - extensionLength));
      })
      .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean test(Identifier location) {
    return resources.contains(location);
  }

  /** Clears the list; saves memory as it can hold many ids */
  public void clear() {
    resources = Set.of();
  }
}
