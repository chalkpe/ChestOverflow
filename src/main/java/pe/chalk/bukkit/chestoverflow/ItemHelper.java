package pe.chalk.bukkit.chestoverflow;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
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
    public static boolean hasNormalInventory(final Block block) {
        final BlockState state = block.getState();
        if (!(state instanceof InventoryHolder holder)) return false;

        final Inventory inventory = holder.getInventory();
        return !(inventory instanceof FurnaceInventory || inventory instanceof BrewerInventory || inventory instanceof BeaconInventory);
    }

    public static Inventory getInventoryFromBlock(final Block block, final Player player) {
        if (block == null) return null;
        if (block.getType() == Material.ENDER_CHEST && player != null) return player.getEnderChest();
        if (block.getState() instanceof InventoryHolder holder && ItemHelper.hasNormalInventory(block)) return holder.getInventory();
        return null;
    }

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

    public static ItemStack getStack(ItemStack stack, int amount) {
        final ItemStack s = stack.clone();
        s.setAmount(amount);
        return s;
    }

    public static void dropItems(final List<ItemStack> stacks, int maxSize, final World world, final Location location) {
        while (stacks.size() > maxSize)
            world.dropItemNaturally(location, stacks.remove(stacks.size() - 1));
    }

}
