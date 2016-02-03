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

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2016-02-04
 */
public class ChestOverflow extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event){
        if(event.isCancelled()) return;
        if(!event.getPlayer().hasPermission("chestoverflow.use")) return;

        if(event.hasItem()) return;
        if(event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        final Block block = event.getClickedBlock();
        if(!(block.getState() instanceof Chest)) return;

        final Chest chest = (Chest) block.getState();
        final Inventory inventory = chest.getInventory();
        if(inventory.getContents() == null || inventory.getSize() <= 0 || inventory.getContents().length <= 0) return;

        final List<ItemStack> stacks = Arrays.stream(inventory.getContents()).collect(Collectors.<ItemStack>toList());
        final List<ItemStack> cleanStacks = ChestOverflow.sortedItemStacks(ChestOverflow.distinctItemStacks(stacks));
        while(cleanStacks.size() > inventory.getMaxStackSize()) block.getWorld().dropItemNaturally(block.getLocation(), cleanStacks.remove(cleanStacks.size() - 1));

        inventory.setContents(cleanStacks.toArray(new ItemStack[inventory.getSize()]));
        chest.update(true);

        event.getPlayer().sendMessage(ChatColor.AQUA + "Your chest has been sorted!");
    }

    public static List<ItemStack> distinctItemStacks(final List<ItemStack> stacks){
        final List<ItemStack> distinctStacks = new ArrayList<>();
        stacks.stream().filter(Objects::nonNull).forEach((final ItemStack stack) -> {
            final Optional<ItemStack> similarStack = distinctStacks.stream()
                    .filter(Objects::nonNull)
                    .filter(theStack -> theStack.isSimilar(stack))
                    .filter(theStack -> theStack.getAmount() < theStack.getMaxStackSize())
                    .findFirst();

            if(similarStack.isPresent()){
                int totalAmount = stack.getAmount() + similarStack.get().getAmount();
                while(totalAmount > stack.getMaxStackSize()){
                    final ItemStack fullStack = stack.clone();
                    fullStack.setAmount(stack.getMaxStackSize());

                    distinctStacks.add(fullStack);
                    totalAmount -= fullStack.getAmount();
                }
                similarStack.get().setAmount(totalAmount);
            }else distinctStacks.add(stack.clone());
        });

        return distinctStacks;
    }

    @SuppressWarnings("deprecation")
    public static final Comparator<ItemStack> COMPARATOR = Comparator.comparingInt(ItemStack::getTypeId)
            .thenComparingInt(stack -> -stack.getEnchantments().values().stream().mapToInt(level -> level * level).sum())
            .thenComparing(stack -> stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName() : null, Comparator.nullsLast(String::compareTo))
            .thenComparingInt(stack -> stack.getItemMeta().hasLore() ? stack.getItemMeta().getLore().size() : Integer.MAX_VALUE)
            .thenComparingInt(ItemStack::getDurability)
            .thenComparingInt(stack -> -stack.getAmount());

    public static List<ItemStack> sortedItemStacks(final List<ItemStack> stacks){
        return stacks.stream().sorted(ChestOverflow.COMPARATOR).collect(Collectors.toList());
    }
}
