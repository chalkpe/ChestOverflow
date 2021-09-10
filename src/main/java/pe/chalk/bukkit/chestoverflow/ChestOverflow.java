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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
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

        final Player player = event.getPlayer();
        if(!player.hasPermission("chestoverflow.use")) return;

        if(event.hasItem()) return;
        if(event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        final Block block = event.getClickedBlock();
        if(block != null && ChestOverflow.handleChest(block, player)){
            event.getPlayer().sendMessage(ChatColor.AQUA + String.format("Your %s has been sorted!", ChestOverflow.isChest(block) ? "chest" : "block's inventory"));
        }
    }

    public static boolean isChest(final Block block){
        return block.getState() instanceof Chest || block.getType() == Material.ENDER_CHEST;
    }

    public static boolean hasNormalInventory(final Block block){
        final BlockState state = block.getState();
        if(!(state instanceof InventoryHolder)) return false;

        final Inventory inventory = ((InventoryHolder) state).getInventory();
        return !(inventory instanceof FurnaceInventory || inventory instanceof BrewerInventory || inventory instanceof BeaconInventory);
    }

    public static Inventory getInventoryFromBlock(final Block block, final Player player){
        if(block == null) return null;
        if(block.getType() == Material.ENDER_CHEST && player != null) return player.getEnderChest();
        if(block.getState() instanceof InventoryHolder && ChestOverflow.hasNormalInventory(block)) return ((InventoryHolder) block.getState()).getInventory();
        return null;
    }

    public static boolean handleChest(final Block block, final Player player){
        final Inventory inventory = getInventoryFromBlock(block, player);
        if(inventory == null || inventory.getSize() <= 0 || inventory.getContents().length <= 0) return false;

        final List<ItemStack> stacks = Arrays.stream(inventory.getContents()).collect(Collectors.<ItemStack>toList());
        final List<ItemStack> cleanStacks = ChestOverflow.sortedItemStacks(ChestOverflow.distinctItemStacks(stacks));
        while(cleanStacks.size() > inventory.getMaxStackSize()) block.getWorld().dropItemNaturally(block.getLocation(), cleanStacks.remove(cleanStacks.size() - 1));

        inventory.setContents(cleanStacks.toArray(new ItemStack[inventory.getSize()]));
        block.getState().update(true);
        return true;
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
    public static final Comparator<ItemStack> COMPARATOR = Comparator.comparing(ItemStack::getType)
            .thenComparingInt(stack -> -stack.getEnchantments().values().stream().mapToInt(level -> level * level).sum())
            .thenComparing(stack -> stack.getItemMeta() != null && stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName() : null, Comparator.nullsLast(String::compareTo))
            .thenComparingInt(stack -> stack.getItemMeta() != null && stack.getItemMeta().hasLore() && stack.getItemMeta().getLore() != null ? stack.getItemMeta().getLore().size() : Integer.MAX_VALUE)
            .thenComparingInt(ItemStack::getDurability)
            .thenComparingInt(stack -> -stack.getAmount());

    public static List<ItemStack> sortedItemStacks(final List<ItemStack> stacks){
        return stacks.stream().sorted(ChestOverflow.COMPARATOR).collect(Collectors.toList());
    }
}
