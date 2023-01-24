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

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author Chalk <chalkpe@gmail.com>
 * @since 2016-02-04
 */
public class ChestOverflow extends JavaPlugin implements Listener, CommandExecutor {
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        Optional.ofNullable(this.getCommand("sort")).ifPresent(cmd -> cmd.setExecutor(this));
    }

    @EventHandler
    public void onItemSort(final @NotNull ItemSortEvent event) {
        if (event.isCancelled()) return;
        event.getTargetPlayer().playSound(event.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 0.5f);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equals("sort")) return true;
        if (!(sender instanceof Player player)) return true;

        Optional.ofNullable(handlePlayer(player))
                .filter(this::triggerItemSortEvent)
                .ifPresent(ItemSorter.DROP_EXCEEDS.andThen(ItemSorter.UPDATE_CHEST));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPermission("chestoverflow.use")) return;
        if (event.hasItem() || event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Optional.ofNullable(handleChest(player, event.getClickedBlock()))
                .filter(this::triggerItemSortEvent)
                .ifPresent(ItemSorter.DROP_EXCEEDS.andThen(ItemSorter.UPDATE_PLAYER));
    }

    public boolean triggerItemSortEvent(final @NotNull ItemSortEvent event) {
        this.getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public static @Nullable ItemSortEvent handlePlayer(final @NotNull Player player) {
        final Inventory inventory = player.getInventory();
        if (inventory.getSize() == 0) return null;

        final ItemStack[] contents = inventory.getStorageContents();
        if (contents.length == 0) return null;

        final List<ItemStack> stacks = Arrays.stream(contents).toList();
        final List<ItemStack> hotbarStacks = stacks.subList(0, 9);
        final List<ItemStack> storageStacks = stacks.subList(9, stacks.size());

        ItemSorter.HOTBAR_FILLER.accept(hotbarStacks, storageStacks);
        final List<ItemStack> sortedStacks = ItemSorter.sortedItemStacks(ItemSorter.distinctItemStacks(storageStacks));
        final List<ItemStack> cleanStacks = Stream.concat(hotbarStacks.stream(), sortedStacks.stream()).toList();

        return new ItemSortEvent(inventory, cleanStacks.toArray(ItemStack[]::new), player, null);
    }

    public static @Nullable ItemSortEvent handleChest(final Player player, final Block block) {
        final Inventory inventory = ItemHelper.getInventoryFromBlock(block, player);
        if (inventory == null || inventory.getSize() == 0) return null;

        final ItemStack[] contents = inventory.getContents();
        if (contents.length == 0) return null;

        final List<ItemStack> stacks = Arrays.stream(contents).toList();
        final List<ItemStack> cleanStacks = ItemSorter.sortedItemStacks(ItemSorter.distinctItemStacks(stacks));

        return new ItemSortEvent(inventory, cleanStacks.toArray(ItemStack[]::new), player, block);
    }
}
