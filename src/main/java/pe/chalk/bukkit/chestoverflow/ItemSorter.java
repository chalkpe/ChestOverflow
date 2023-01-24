package pe.chalk.bukkit.chestoverflow;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ItemSorter {
    private ItemSorter() {}

    public static final Consumer<ItemSortEvent> DROP_EXCEEDS = event -> {
        final var maxSize = event.getInventory().getSize();
        final var contents = event.getContents();
        if (contents.length <= maxSize) return;

        final var location = event.getLocation();
        final var world = event.getTargetPlayer().getWorld();
        final var stacks = new ArrayList<>(Arrays.asList(contents));

        while (stacks.size() > maxSize) world.dropItemNaturally(location, stacks.remove(stacks.size() - 1));
        event.setContents(stacks.toArray(ItemStack[]::new));
    };

    public static final Consumer<ItemSortEvent> UPDATE_PLAYER = event -> {
        event.getInventory().setStorageContents(event.getContents());
        event.getTargetPlayer().updateInventory();
    };

    public static final Consumer<ItemSortEvent> UPDATE_CHEST = event -> {
        event.getInventory().clear();
        event.getInventory().setContents(event.getContents());
    };

    public static BiConsumer<List<ItemStack>, List<ItemStack>> HOTBAR_FILLER = (hotbar, storage) ->
            hotbar.stream()
                    .filter(Objects::nonNull)
                    .forEach(slot -> storage.stream()
                            .filter(slot::isSimilar)
                            .filter(inv -> inv.getAmount() > 0)
                            .takeWhile(inv -> slot.getAmount() < slot.getMaxStackSize())
                            .forEach(inv -> ItemHelper.moveAmount(inv, slot)));

    public static final BiConsumer<Map<ItemStack, Integer>, Map.Entry<ItemStack, Integer>> SIMILAR_ACCUMULATOR = (map, entry) ->
            map.merge(ItemHelper.findKey(map.keySet(), entry.getKey()), entry.getValue(), Integer::sum);

    public static final Collector<ItemStack, Map<ItemStack, Integer>, List<ItemStack>> DISTINCT_COLLECTOR = Collector.of(
            HashMap::new,
            (map, stack) -> SIMILAR_ACCUMULATOR.accept(map, Map.entry(stack, stack.getAmount())),
            (mapA, mapB) -> new HashMap<>() {{ Stream.of(mapA, mapB).forEach(map -> map.entrySet().forEach(entry -> SIMILAR_ACCUMULATOR.accept(this, entry))); }},
            map -> map.entrySet().stream().flatMap(ItemHelper::generateStacks).toList()
    );

    public static List<ItemStack> distinctItemStacks(final @NotNull List<ItemStack> stacks) {
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

    public static List<ItemStack> sortedItemStacks(final @NotNull List<ItemStack> stacks) {
        return stacks.stream().sorted(COMPARATOR).collect(Collectors.toList());
    }
}
