package pe.chalk.bukkit.chestoverflow;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
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
public final class ItemHelper {
    private ItemHelper() {}

    public static final List<Class<? extends Inventory>> INVALID_INVENTORY_CLASS = List.of(FurnaceInventory.class, BrewerInventory.class, BeaconInventory.class);

    public static Inventory getInventoryFromBlock(final Block block, final Player player) {
        if (block == null) return null;
        if (block.getType() == Material.ENDER_CHEST) return player.getEnderChest();
        if (!(block.getState() instanceof Container container)) return null;

        final Inventory inventory = container.getInventory();
        return INVALID_INVENTORY_CLASS.contains(inventory.getClass()) ? null : inventory;
    }

    public static Map<Enchantment, Integer> getStoredEnchants(ItemMeta meta) {
        return meta instanceof EnchantmentStorageMeta ? ((EnchantmentStorageMeta) meta).getStoredEnchants() : Map.of();
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

    public static ItemStack findKey(Collection<ItemStack> stacks, ItemStack stack) {
        return stacks.stream().filter(stack::isSimilar).findFirst().orElse(stack);
    }

    public static ItemStack generateAmountStack(ItemStack stack, int amount) {
        final ItemStack s = stack.clone();
        s.setAmount(amount);
        return s;
    }

    public static Stream<ItemStack> generateStacks(Map.Entry<ItemStack, Integer> entry) {
        return generateStacks(entry, entry.getKey().getMaxStackSize());
    }

    public static Stream<ItemStack> generateStacks(Map.Entry<ItemStack, Integer> entry, int maxStackSize) {
        final ItemStack stack = entry.getKey();
        final int amount = entry.getValue();

        final int bundles = amount / maxStackSize;
        final int remaining = amount % maxStackSize;

        return Stream.concat(
                IntStream.range(0, bundles).mapToObj(i -> generateAmountStack(stack, maxStackSize)),
                remaining > 0 ? Stream.of(generateAmountStack(stack, remaining)) : Stream.empty()
        );
    }

    public static void moveAmount(ItemStack from, ItemStack to) {
        if (from == null || !from.isSimilar(to)) return;
        if (to.getAmount() == to.getMaxStackSize()) return;

        final int maxMovingAmount = to.getMaxStackSize() - to.getAmount();
        final int movingAmount = Math.min(from.getAmount(), maxMovingAmount);

        to.setAmount(to.getAmount() + movingAmount);
        from.setAmount(from.getAmount() - movingAmount);
    }
}
