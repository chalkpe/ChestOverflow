package pe.chalk.bukkit.chestoverflow;

import com.google.common.collect.ImmutableMap;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/**
 * @author Chalk <chalkpe@gmail.com>
 * @since 2023-01-16
 */
public class ItemHelper {
    public static Map<Enchantment, Integer> getStoredEnchants(ItemMeta meta) {
        return meta instanceof EnchantmentStorageMeta ? ((EnchantmentStorageMeta) meta).getStoredEnchants() : ImmutableMap.of();
    }

    public static Integer getDamage(ItemMeta meta) {
        return ((Damageable) meta).hasDamage() ? ((Damageable) meta).getDamage() : null;
    }

    public static String getDisplayName(ItemMeta meta) {
        return meta.hasDisplayName() ? meta.getDisplayName() : null;
    }

    public static List<String> getLore(ItemMeta meta) {
        return meta.hasLore() ? meta.getLore() : null;
    }
}
