/*
 * Copyright (C) 2016  ChalkPE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pe.chalk.bukkit.chestoverflow;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Chalk <chalkpe@gmail.com>
 * @since 2016-02-04
 */
public class ChestOverflow extends JavaPlugin implements Listener, CommandExecutor {
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        final PluginCommand command = this.getCommand("sort");
        if (command != null) command.setExecutor(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPermission("chestoverflow.use")) return;
        if (event.hasItem() || event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        final Block block = event.getClickedBlock();
        if (block != null && handleChest(block, player)) playSound(player, block.getLocation());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equals("sort")) return true;
        if (!(sender instanceof Player player)) return true;
        if (handlePlayer(player)) playSound(player, player.getLocation());
        return true;
    }

    public static void playSound(final Player player, final Location location) {
        player.playSound(location, Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 0.5f);
    }

    public static boolean handleChest(final Block block, final Player player) {
        final Inventory inventory = ItemHelper.getInventoryFromBlock(block, player);
        if (inventory == null || inventory.getSize() <= 0 || inventory.getContents().length == 0) return false;

        final List<ItemStack> stacks = Arrays.stream(inventory.getContents()).collect(Collectors.toList());
        final List<ItemStack> cleanStacks = ChestOverflow.sortedItemStacks(ChestOverflow.distinctItemStacks(stacks));

        ItemHelper.dropItems(cleanStacks, inventory.getMaxStackSize(), block.getWorld(), block.getLocation());
        inventory.setContents(cleanStacks.toArray(ItemStack[]::new));
        block.getState().update(true);
        return true;
    }

    public static BiConsumer<List<ItemStack>, List<ItemStack>> HOTBAR_FILLER = (hotbar, storage) ->
            hotbar.stream()
                    .filter(Objects::nonNull)
                    .forEach(slot -> storage.stream()
                            .filter(slot::isSimilar)
                            .filter(inv -> inv.getAmount() > 0)
                            .takeWhile(inv -> slot.getAmount() < slot.getMaxStackSize())
                            .forEach(inv -> ItemHelper.moveAmount(inv, slot)));

    public static boolean handlePlayer(final Player player) {
        final Inventory inventory = player.getInventory();
        if (inventory.getSize() <= 0 || inventory.getStorageContents().length == 0) return false;

        final List<ItemStack> stacks = Arrays.stream(inventory.getStorageContents()).collect(Collectors.toList());
        final List<ItemStack> hotbarStacks = stacks.subList(0, 9);
        final List<ItemStack> storageStacks = stacks.subList(9, stacks.size());

        HOTBAR_FILLER.accept(hotbarStacks, storageStacks);
        final List<ItemStack> sortedStacks = ChestOverflow.sortedItemStacks(ChestOverflow.distinctItemStacks(storageStacks));
        final List<ItemStack> cleanStacks = Stream.concat(hotbarStacks.stream(), sortedStacks.stream()).collect(Collectors.toList());

        ItemHelper.dropItems(cleanStacks, inventory.getMaxStackSize(), player.getWorld(), player.getLocation());
        inventory.setStorageContents(cleanStacks.toArray(ItemStack[]::new));
        player.updateInventory();
        return true;
    }

    public static final BiConsumer<Map<ItemStack, Integer>, Map.Entry<ItemStack, Integer>> SIMILAR_ACCUMULATOR = (map, entry) ->
            map.merge(ItemHelper.findKey(map.keySet(), entry.getKey()), entry.getValue(), Integer::sum);

    public static final Collector<ItemStack, Map<ItemStack, Integer>, List<ItemStack>> DISTINCT_COLLECTOR = Collector.of(
            HashMap::new,
            (map, stack) -> SIMILAR_ACCUMULATOR.accept(map, Map.entry(stack, stack.getAmount())),
            (mapA, mapB) -> {
                final Map<ItemStack, Integer> result = new HashMap<>();
                Stream.of(mapA, mapB).forEach(map -> map.entrySet().forEach(entry -> SIMILAR_ACCUMULATOR.accept(result, entry)));
                return result;
            },
            map -> map.entrySet().stream().flatMap(ItemHelper::generateStacks).toList()
    );

    public static List<ItemStack> distinctItemStacks(final List<ItemStack> stacks) {
        return stacks.stream()
                .filter(Objects::nonNull)
                .filter(stack -> stack.getAmount() > 0)
                .collect(DISTINCT_COLLECTOR);
    }

    public static final Comparator<String> ENCHANTM_COMPARATOR = Comparator
            .<String>comparingInt(e -> e.contains("curse") ? 1 : 0)
            .thenComparing(String::compareTo);

    public static final Comparator<Map<Enchantment, Integer>> ENCHANTS_COMPARATOR = Comparator
            .comparing(Map<Enchantment, Integer>::size, Comparator.reverseOrder())
            .thenComparing(enchants -> enchants.values().stream().mapToInt(level -> level * level).sum(), Comparator.reverseOrder())
            .thenComparing(enchants -> enchants.keySet().stream().map(enchant -> enchant.getKey().toString()).sorted(ENCHANTM_COMPARATOR).collect(Collectors.joining(",")));

    public static final Comparator<List<String>> LORE_COMPARATOR = Comparator
            .comparing(List<String>::size, Comparator.reverseOrder())
            .thenComparing(list -> String.join("\n", list));

    public static final Comparator<ItemMeta> META_COMPARATOR = Comparator
            .comparing(ItemMeta::getEnchants, ENCHANTS_COMPARATOR)
            .thenComparing(ItemHelper::getStoredEnchants, ENCHANTS_COMPARATOR)
            .thenComparing(ItemHelper::getDisplayName, Comparator.nullsLast(String::compareTo))
            .thenComparing(ItemHelper::getLore, Comparator.nullsLast(LORE_COMPARATOR))
            .thenComparing(ItemHelper::getDamage, Comparator.nullsFirst(Integer::compareTo));

    public static final Comparator<ItemStack> COMPARATOR = Comparator
            .comparing(ItemStack::getType)
            .thenComparing(ItemStack::getItemMeta, Comparator.nullsLast(META_COMPARATOR))
            .thenComparing(ItemStack::getAmount, Comparator.reverseOrder());

    public static List<ItemStack> sortedItemStacks(final List<ItemStack> stacks) {
        return stacks.stream().sorted(ChestOverflow.COMPARATOR).collect(Collectors.toList());
    }
}
