package slimeknights.mantle.util;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Helpers for translation keys and tooltips.
 * Uses vanilla's {@link Language}, which replaces Forge's ForgeI18n; on a dedicated server it only knows English.
 */
@SuppressWarnings("WeakerAccess")
public final class TranslationHelper {
  private TranslationHelper() {}

  /** Formats numbers with thousands separators and up to two decimals, such as {@code 1,234.5} */
  public static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###,###.##", DecimalFormatSymbols.getInstance(Locale.US));

  /** Checks if the current language has the key */
  public static boolean canTranslate(String key) {
    return Language.getInstance().has(key);
  }

  /** Checks if a translation attempt produced text, since a missing key translates to itself */
  public static boolean canTranslate(String key, String attempted) {
    return !key.equals(attempted);
  }

  /** Adds the stack's {@code <description id>.tooltip} lines when translated */
  public static void addOptionalTooltip(ItemStack stack, List<Component> tooltip) {
    addOptionalTooltip(stack.getItem().getDescriptionId() + ".tooltip", tooltip);
  }

  /** Adds the key's translated lines when translated */
  public static void addOptionalTooltip(String key, List<Component> tooltip) {
    String translated = Language.getInstance().getOrDefault(key);
    if (canTranslate(key, translated)) {
      addEachLine(translated, tooltip);
    }
  }

  /** Adds each line of the text as a gray tooltip line */
  public static void addEachLine(String text, List<Component> tooltip) {
    for (String line : text.split("\n")) {
      tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
  }

  /** Replaces escaped {@code \n} sequences with real newlines */
  public static @Nullable String convertNewlines(@Nullable String line) {
    if (line == null) {
      return null;
    }
    return line.replace("\\n", "\n");
  }
}
