package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map.Entry;
import java.util.Optional;

/**
 * Loadable for a block state: a block id for the default state, or {@code {"block": id, "properties": {...}}}.
 * The two values differ only in which properties they write.
 */
public enum BlockStateLoadable implements RecordLoadable<BlockState> {
  /** Writes every property */
  ALL {
    @Override
    protected <T extends Comparable<T>> void serializeProperty(BlockState state, Property<T> property, BlockState defaultState, JsonObject json) {
      json.addProperty(property.getName(), property.getName(state.getValue(property)));
    }
  },
  /** Writes only properties that differ from the default state, and a plain id for the default state */
  DIFFERENCE {
    @Override
    protected <T extends Comparable<T>> void serializeProperty(BlockState state, Property<T> property, BlockState defaultState, JsonObject json) {
      T value = state.getValue(property);
      if (!value.equals(defaultState.getValue(property))) {
        json.addProperty(property.getName(), property.getName(value));
      }
    }
  };

  /** Writes one property if this mode includes it */
  protected abstract <T extends Comparable<T>> void serializeProperty(BlockState state, Property<T> property, BlockState defaultState, JsonObject json);

  @Override
  public BlockState convert(JsonElement element, String key, TypedMap context) {
    if (element.isJsonPrimitive()) {
      return Loadables.BLOCK.convert(element, key, context).defaultBlockState();
    }
    return RecordLoadable.super.convert(element, key, context);
  }

  /** Sets a property from its value name */
  private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String name) {
    Optional<T> value = property.getValue(name);
    if (value.isEmpty()) {
      throw new JsonSyntaxException("Property " + property + " does not contain value " + name);
    }
    return state.setValue(property, value.get());
  }

  @Override
  public BlockState deserialize(JsonObject json, TypedMap context) {
    Block block = Loadables.BLOCK.getIfPresent(json, "block", context);
    BlockState state = block.defaultBlockState();
    if (!json.has("properties")) {
      return state;
    }
    StateDefinition<Block, BlockState> definition = block.getStateDefinition();
    for (Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "properties").entrySet()) {
      String key = entry.getKey();
      Property<?> property = definition.getProperty(key);
      if (property == null) {
        throw new JsonSyntaxException("Property " + key + " does not exist in block " + block);
      }
      state = setValue(state, property, GsonHelper.convertToString(entry.getValue(), key));
    }
    return state;
  }

  @Override
  public JsonElement serialize(BlockState state) {
    Block block = state.getBlock();
    if (this == DIFFERENCE && state == block.defaultBlockState()) {
      return Loadables.BLOCK.serialize(block);
    }
    return RecordLoadable.super.serialize(state);
  }

  @Override
  public void serialize(BlockState state, JsonObject json) {
    Block block = state.getBlock();
    json.add("block", Loadables.BLOCK.serialize(block));
    BlockState defaultState = block.defaultBlockState();
    JsonObject properties = new JsonObject();
    for (Property<?> property : block.getStateDefinition().getProperties()) {
      serializeProperty(state, property, defaultState, properties);
    }
    if (!properties.isEmpty()) {
      json.add("properties", properties);
    }
  }

  @Override
  public BlockState decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return Block.stateById(buffer.readVarInt());
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, BlockState object) {
    buffer.writeVarInt(Block.getId(object));
  }
}
