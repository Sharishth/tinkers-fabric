package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Loadables for item stacks.
 * JSON is {@code "item_id"} or {@code {"item": id, "count": n, "components": {...}}}; the components object
 * is vanilla's component patch, replacing upstream's "nbt" key (26.x moved item data to components, D-21).
 * OPTIONAL forms allow an empty stack (air or count 0); REQUIRED forms reject it.
 */
@SuppressWarnings("unused")  // API
public final class ItemStackLoadable {
  private ItemStackLoadable() {}

  private static final Function<ItemStack, Item> ITEM_GETTER = ItemStack::getItem;
  private static final BiFunction<ItemStack, ErrorFactory, ItemStack> NOT_EMPTY = (stack, error) -> {
    if (stack.isEmpty()) {
      throw error.create("ItemStack cannot be empty");
    }
    return stack;
  };

  private static final LoadableField<Item, ItemStack> ITEM = Loadables.ITEM.defaultField("item", Items.AIR, false, ITEM_GETTER);
  private static final LoadableField<Integer, ItemStack> COUNT = IntLoadable.FROM_ZERO.defaultField("count", 1, true, ItemStack::getCount);
  private static final LoadableField<DataComponentPatch, ItemStack> COMPONENTS = new CodecLoadable<>(DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC)
    .defaultField("components", DataComponentPatch.EMPTY, false, ItemStack::getComponentsPatch);

  /* Optional */
  /** Item id only; count 1 */
  public static final Loadable<ItemStack> OPTIONAL_ITEM = Loadables.ITEM.map(item -> makeStack(item, 1, DataComponentPatch.EMPTY), ITEM_GETTER);
  /** Item and count; written as the id when the count is 1 */
  public static final RecordLoadable<ItemStack> OPTIONAL_STACK = RecordLoadable.create(ITEM, COUNT, (item, count) -> makeStack(item, count, DataComponentPatch.EMPTY))
    .compact(OPTIONAL_ITEM, stack -> stack.getCount() == 1);
  /** Item and components; count 1. Upstream's OPTIONAL_ITEM_NBT */
  public static final RecordLoadable<ItemStack> OPTIONAL_ITEM_COMPONENTS = ComponentStack.FIXED_COUNT;
  /** Item, count and components. Upstream's OPTIONAL_STACK_NBT */
  public static final RecordLoadable<ItemStack> OPTIONAL_STACK_COMPONENTS = ComponentStack.READ_COUNT;

  /* Required */
  public static final Loadable<ItemStack> REQUIRED_ITEM = notEmpty(OPTIONAL_ITEM);
  public static final RecordLoadable<ItemStack> REQUIRED_STACK = notEmpty(OPTIONAL_STACK);
  public static final RecordLoadable<ItemStack> REQUIRED_ITEM_COMPONENTS = notEmpty(OPTIONAL_ITEM_COMPONENTS);
  public static final RecordLoadable<ItemStack> REQUIRED_STACK_COMPONENTS = notEmpty(OPTIONAL_STACK_COMPONENTS);

  /** Creates a stack, or the empty stack for air or a count of 0 */
  private static ItemStack makeStack(Item item, int count, DataComponentPatch components) {
    if (item == Items.AIR || count == 0) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = new ItemStack(item, count);
    stack.applyComponents(components);
    return stack;
  }

  /** Makes the loadable reject empty stacks */
  public static Loadable<ItemStack> notEmpty(Loadable<ItemStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }

  /** Makes the record loadable reject empty stacks */
  public static RecordLoadable<ItemStack> notEmpty(RecordLoadable<ItemStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }

  /** Stack with components, with or without a count */
  private enum ComponentStack implements RecordLoadable<ItemStack> {
    READ_COUNT,
    FIXED_COUNT;

    @Override
    public ItemStack deserialize(JsonObject json, TypedMap context) {
      int count = this == READ_COUNT ? COUNT.get(json, context) : 1;
      return makeStack(ITEM.get(json, context), count, COMPONENTS.get(json, context));
    }

    @Override
    public void serialize(ItemStack stack, JsonObject json) {
      ITEM.serialize(stack, json);
      if (this == READ_COUNT) {
        COUNT.serialize(stack, json);
      }
      COMPONENTS.serialize(stack, json);
    }

    @Override
    public ItemStack convert(JsonElement element, String key, TypedMap context) {
      if (element.isJsonPrimitive()) {
        return OPTIONAL_ITEM.convert(element, key, context);
      }
      return RecordLoadable.super.convert(element, key, context);
    }

    @Override
    public JsonElement serialize(ItemStack stack) {
      if ((this == FIXED_COUNT || stack.getCount() == 1) && stack.getComponentsPatch().isEmpty()) {
        return OPTIONAL_ITEM.serialize(stack);
      }
      return RecordLoadable.super.serialize(stack);
    }

    // WHY vanilla's codec: it sends the component patch the way the client expects, including count
    @Override
    public ItemStack decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, ItemStack stack) {
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
    }
  }
}
