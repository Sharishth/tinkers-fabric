package slimeknights.mantle.data.predicate.damage;

import net.minecraft.world.damagesource.DamageSource;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Predicate matching a damage source's message id, such as "arrow".
 * @param message  Message id
 */
public record SourceMessagePredicate(String message) implements DamageSourcePredicate {
  public static final RecordLoadable<SourceMessagePredicate> LOADER = RecordLoadable.create(
    StringLoadable.DEFAULT.requiredField("message", SourceMessagePredicate::message), SourceMessagePredicate::new);

  /** Creates a predicate matching the message of the given source */
  public SourceMessagePredicate(DamageSource source) {
    this(source.getMsgId());
  }

  @Override
  public boolean matches(DamageSource source) {
    return message.equals(source.getMsgId());
  }

  @Override
  public RecordLoadable<SourceMessagePredicate> getLoader() {
    return LOADER;
  }
}
