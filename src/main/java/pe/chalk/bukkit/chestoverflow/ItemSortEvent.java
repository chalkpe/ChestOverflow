package pe.chalk.bukkit.chestoverflow;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemSortEvent extends Event implements Cancellable {
    private static final HandlerList LIST = new HandlerList();

    private final Inventory inventory;
    private final Player targetPlayer;
    private final @Nullable Block targetBlock;

    private ItemStack[] contents;
    private boolean cancelled = false;

    public ItemSortEvent(Inventory inventory, ItemStack[] contents, Player targetPlayer, @Nullable Block targetBlock) {
        super();
        this.inventory = inventory;
        this.contents = contents;
        this.targetPlayer = targetPlayer;
        this.targetBlock = targetBlock;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    @SuppressWarnings("unused")
    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public Optional<Block> getTargetBlock() {
        return Optional.ofNullable(targetBlock);
    }

    public Location getLocation() {
        return this.getTargetBlock()
                .map(Block::getLocation)
                .orElseGet(() -> this.getTargetPlayer().getLocation());
    }
}
