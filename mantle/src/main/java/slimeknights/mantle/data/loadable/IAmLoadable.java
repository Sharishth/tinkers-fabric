package slimeknights.mantle.data.loadable;

import slimeknights.mantle.data.loadable.record.RecordLoadable;

/** Object that knows the loadable that reads and writes it */
public interface IAmLoadable {
  /** Gets the loadable for this object; it must accept this object */
  Loadable<? extends IAmLoadable> loadable();

  /** Object whose loadable is a record loadable */
  interface Record extends IAmLoadable {
    @Override
    RecordLoadable<? extends IAmLoadable.Record> loadable();
  }
}
