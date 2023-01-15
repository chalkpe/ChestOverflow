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

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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

import java.util.*;
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
    public void onPlayerInteract(PlayerInteractEvent event){
        final Player player = event.getPlayer();
        if(!player.hasPermission("chestoverflow.use")) return;

        if(event.hasItem()) return;
        if(event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        final Block block = event.getClickedBlock();
        if(block != null && ChestOverflow.handleChest(block, player)){
            player.playSound(block.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 0.5f);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        if (!command.getName().equals("sort")) return true;

        final Player player = (Player) sender;
        if (ChestOverflow.handlePlayer(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 0.5f);
        }
        return true;
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
        if(inventory == null || inventory.getSize() <= 0 || inventory.getContents().length == 0) return false;

        final List<ItemStack> stacks = Arrays.stream(inventory.getContents()).collect(Collectors.toList());
        final List<ItemStack> cleanStacks = ChestOverflow.sortedItemStacks(ChestOverflow.distinctItemStacks(stacks));
        while(cleanStacks.size() > inventory.getMaxStackSize()) block.getWorld().dropItemNaturally(block.getLocation(), cleanStacks.remove(cleanStacks.size() - 1));

        inventory.setContents(cleanStacks.toArray(ItemStack[]::new));
        block.getState().update(true);
        return true;
    }

    public static boolean handlePlayer(final Player player) {
        final Inventory inventory = player.getInventory();
        final List<ItemStack> stacks = Arrays.stream(inventory.getStorageContents()).collect(Collectors.toList());

        final List<ItemStack> hotbarStacks = stacks.subList(0, 9);
        final List<ItemStack> storageStacks = stacks.subList(9, stacks.size());
        final List<ItemStack> sortedStacks = ChestOverflow.sortedItemStacks(ChestOverflow.distinctItemStacks((storageStacks)));
        final List<ItemStack> cleanStacks = Stream.concat(hotbarStacks.stream(), sortedStacks.stream()).collect(Collectors.toList());

        while(cleanStacks.size() > inventory.getMaxStackSize()) player.getWorld().dropItemNaturally(player.getLocation(), cleanStacks.remove(cleanStacks.size() - 1));
        inventory.setStorageContents(cleanStacks.toArray(ItemStack[]::new));
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

    public static List<ItemStack> sortedItemStacks(final List<ItemStack> stacks){
        return stacks.stream().sorted(ChestOverflow.COMPARATOR).collect(Collectors.toList());
    }
}
