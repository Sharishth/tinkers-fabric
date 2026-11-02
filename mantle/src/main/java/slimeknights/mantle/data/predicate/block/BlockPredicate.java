package slimeknights.mantle.data.predicate.block;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.RegistryPredicateRegistry;

import java.util.List;
import java.util.function.Predicate;

/** Predicate over block states */
public interface BlockPredicate extends IJsonPredicate<BlockState> {
  /** Matches every state */
  BlockPredicate ANY = simple(state -> true);
  /** Matches no state */
  BlockPredicate NONE = simple(state -> false);
  /** Registry of block predicate types */
  RegistryPredicateRegistry<Block, BlockState> LOADER = new RegistryPredicateRegistry<>(
    "Block Predicate", ANY, NONE, Loadables.BLOCK, BlockState::getBlock, "blocks", Loadables.BLOCK_TAG, (tag, state) -> state.is(tag));

  /* Singletons */
  BlockPredicate REQUIRES_TOOL = simple(BlockState::requiresCorrectToolForDrops);
  /** Blocks entities walk into rather than through */
  BlockPredicate BLOCKS_MOTION = simple(BlockPredicate::blocksMotion);
  BlockPredicate CAN_BE_REPLACED = simple(BlockState::canBeReplaced);

  @Override
  default IJsonPredicate<BlockState> inverted() {
    return LOADER.invert(this);
  }

  /**
   * Checks if the state blocks movement.
   * WHY deprecated isSolid: 26.x removed BlockStateBase.blocksMotion, whose 1.20 body was exactly this;
   * isSolid is deprecated as legacy but still what vanilla uses for the same check.
   */
  @SuppressWarnings("deprecation")
  private static boolean blocksMotion(BlockState state) {
    return !state.is(Blocks.COBWEB) && !state.is(Blocks.BAMBOO_SAPLING) && state.isSolid();
  }

  /** Creates a fieldless predicate with its own loader */
  static BlockPredicate simple(Predicate<BlockState> predicate) {
    return SingletonLoader.singleton(loader -> new BlockPredicate() {
      @Override
      public boolean matches(BlockState state) {
        return predicate.test(state);
      }

      @Override
      public RecordLoadable<? extends BlockPredicate> getLoader() {
        return loader;
      }
    });
  }

  /** Creates a predicate matching any of the blocks */
  static IJsonPredicate<BlockState> set(Block... blocks) {
    return LOADER.setOf(blocks);
  }

  /** Creates a predicate matching blocks in the tag */
  static IJsonPredicate<BlockState> tag(TagKey<Block> tag) {
    return LOADER.tag(tag);
  }

  /** Creates a predicate matching when all match */
  @SafeVarargs
  static IJsonPredicate<BlockState> and(IJsonPredicate<BlockState>... predicates) {
    return LOADER.and(List.of(predicates));
  }

  /** Creates a predicate matching when any matches */
  @SafeVarargs
  static IJsonPredicate<BlockState> or(IJsonPredicate<BlockState>... predicates) {
    return LOADER.or(List.of(predicates));
  }
}
