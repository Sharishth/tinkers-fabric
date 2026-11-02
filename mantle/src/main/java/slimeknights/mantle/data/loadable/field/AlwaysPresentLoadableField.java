package slimeknights.mantle.data.loadable.field;

/** Loadable field whose value always exists, so the network form is just the value */
public interface AlwaysPresentLoadableField<T, P> extends LoadableField<T, P>, AlwaysPresentRecordField<T, P> {}
