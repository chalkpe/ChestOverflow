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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    public static void dropItems(final List<ItemStack> stacks, int maxSize, final World world, final Location location) {
        while (stacks.size() > maxSize)
            world.dropItemNaturally(location, stacks.remove(stacks.size() - 1));
    }

    public static ItemStack findKey(Collection<ItemStack> stacks, ItemStack stack) {
        return stacks.stream().filter(stack::isSimilar).findFirst().orElse(stack);
    }

    public static ItemStack generateAmountStack(ItemStack stack, int amount) {
        final ItemStack s = stack.clone();
        s.setAmount(amount);
        return s;
    }

    public static Stream<ItemStack> generateStacks(Map.Entry<ItemStack, Integer> entry) {
        final ItemStack stack = entry.getKey();
        final int amount = entry.getValue();
        final int maxStackSize = stack.getMaxStackSize();

        final int bundles = amount / maxStackSize;
        final int remaining = amount % maxStackSize;

        return Stream.concat(
                IntStream.range(0, bundles).mapToObj(i -> generateAmountStack(stack, maxStackSize)),
                remaining > 0 ? Stream.of(generateAmountStack(stack, remaining)) : Stream.empty()
        );
    }
}
