package slimeknights.mantle.data.listener;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reload listener that runs in the prepare phase.
 * Like {@link net.minecraft.server.packs.resources.ResourceManagerReloadListener}, but off the game thread and before
 * any listener applies; models load during the prepare phase, so data they read has to be ready by then.
 * The body must be thread safe and must not touch game state.
 * Also replaces upstream's IEarlySafeManagerReloadListener: Fabric stops the game on a mod loading error, so there
 * is no broken loading state left to guard against.
 */
public interface IEarlyReloadListener extends PreparableReloadListener {
  @Override
  default CompletableFuture<Void> reload(SharedState state, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
    ResourceManager resourceManager = state.resourceManager();
    return CompletableFuture.runAsync(() -> onResourceManagerReload(resourceManager), prepareExecutor).thenCompose(barrier::wait);
  }

  /**
   * Reads the resources; runs on a background thread.
   * @param resourceManager  Resource manager being reloaded
   */
  void onResourceManagerReload(ResourceManager resourceManager);
}
