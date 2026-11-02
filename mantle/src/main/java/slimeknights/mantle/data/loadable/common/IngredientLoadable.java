package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;

/**
 * Loadable for a vanilla ingredient, in 26.x's JSON format (an item id, {@code "#tag"}, or a list).
 * 26.x ingredients cannot be empty, so upstream's ALLOW_EMPTY is {@link #OPTIONAL}, matching how vanilla models it.
 */
public enum IngredientLoadable implements Loadable<Ingredient> {
  DISALLOW_EMPTY;

  /** Ingredient that may be absent: an empty JSON array, or no ingredient */
  public static final Loadable<Optional<Ingredient>> OPTIONAL = new OptionalIngredient();

  /** Codec form, sharing the registry-aware JSON parsing */
  private static final CodecLoadable<Ingredient> CODEC = new CodecLoadable<>(Ingredient.CODEC, Ingredient.CONTENTS_STREAM_CODEC);

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    return CODEC.convert(element, key, context);
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    return CODEC.serialize(object);
  }

  @Override
  public Ingredient decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    return Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, Ingredient object) {
    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, object);
  }

  /** Ingredient or nothing; an empty JSON array means nothing */
  private static final class OptionalIngredient implements Loadable<Optional<Ingredient>> {
    @Override
    public Optional<Ingredient> convert(JsonElement element, String key, TypedMap context) {
      if (element.isJsonArray() && element.getAsJsonArray().isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(DISALLOW_EMPTY.convert(element, key, context));
    }

    @Override
    public JsonElement serialize(Optional<Ingredient> object) {
      return object.map(DISALLOW_EMPTY::serialize).orElseGet(JsonArray::new);
    }

    @Override
    public Optional<Ingredient> decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Optional<Ingredient> object) {
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, object);
    }
  }
}
