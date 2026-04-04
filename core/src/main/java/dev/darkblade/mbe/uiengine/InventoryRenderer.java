package dev.darkblade.mbe.uiengine;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryRenderer {
    private final Map<String, InventoryDataProvider> providers;
    private final Map<String, Map<Integer, Character>> slotSymbolsCache = new ConcurrentHashMap<>();

    public InventoryRenderer(Map<String, InventoryDataProvider> providers) {
        this.providers = providers == null ? Map.of() : Map.copyOf(providers);
    }

    public Inventory render(Player player, InventoryViewDefinition view) {
        return renderWithBindings(player, view).inventory();
    }

    public RenderResult renderWithBindings(Player player, InventoryViewDefinition view) {
        Objects.requireNonNull(view, "view");
        Inventory inventory = Bukkit.createInventory(new ViewHolder(view.id()), view.size(), color(view.title()));
        Map<Integer, Object> bindings = new LinkedHashMap<>();
        Map<Character, List<Integer>> dynamicSlots = collectDynamicSlots(view);
        Map<Integer, Character> symbolsBySlot = resolveSymbolsBySlot(view);

        for (Map.Entry<Integer, Character> entry : symbolsBySlot.entrySet()) {
            int slot = entry.getKey();
            char symbol = entry.getValue();
            if (dynamicSlots.containsKey(symbol)) {
                continue;
            }
            InventoryItemDefinition item = view.items().get(symbol);
            if (item == null) {
                continue;
            }
            inventory.setItem(slot, createItem(item, null, 0));
        }

        for (DynamicSection section : view.dynamicSections()) {
            List<Integer> slots = dynamicSlots.getOrDefault(section.symbol(), List.of());
            if (slots.isEmpty()) {
                continue;
            }
            InventoryDataProvider provider = providers.get(section.providerId());
            if (provider == null) {
                continue;
            }
            List<?> data = provider.provide(player);
            if (data == null || data.isEmpty()) {
                continue;
            }
            InventoryItemDefinition template = view.items().get(section.symbol());
            int max = Math.min(slots.size(), data.size());
            for (int i = 0; i < max; i++) {
                int slot = slots.get(i);
                Object row = data.get(i);
                bindings.put(slot, row);
                inventory.setItem(slot, createItem(template, row, i));
            }
        }
        return new RenderResult(inventory, Map.copyOf(bindings));
    }

    private Map<Character, List<Integer>> collectDynamicSlots(InventoryViewDefinition view) {
        Map<Character, List<Integer>> out = new LinkedHashMap<>();
        Map<Integer, Character> symbolsBySlot = resolveSymbolsBySlot(view);
        for (DynamicSection section : view.dynamicSections()) {
            List<Integer> slots = new ArrayList<>();
            for (Map.Entry<Integer, Character> entry : symbolsBySlot.entrySet()) {
                if (entry.getValue() == section.symbol()) {
                    slots.add(entry.getKey());
                }
            }
            out.put(section.symbol(), List.copyOf(slots));
        }
        return out;
    }

    private Map<Integer, Character> resolveSymbolsBySlot(InventoryViewDefinition view) {
        return slotSymbolsCache.computeIfAbsent(view.id(), key -> {
            Map<Integer, Character> map = new LinkedHashMap<>();
            int slot = 0;
            for (String row : view.layout()) {
                if (row == null) {
                    continue;
                }
                for (int i = 0; i < row.length() && slot < view.size(); i++) {
                    char symbol = row.charAt(i);
                    if (symbol != ' ' && symbol != '.') {
                        map.put(slot, symbol);
                    }
                    slot++;
                }
                if (slot >= view.size()) {
                    break;
                }
            }
            return Map.copyOf(map);
        });
    }

    private ItemStack createItem(InventoryItemDefinition template, Object data, int index) {
        InventoryItemDefinition resolved = template == null
                ? defaultTemplate(data)
                : template;
        Material material = resolved.material() == null ? Material.STONE : resolved.material();
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        String name = interpolate(resolved.name(), data, index);
        if (!name.isBlank()) {
            meta.setDisplayName(color(name));
        }
        List<String> lore = resolved.lore();
        if (!lore.isEmpty()) {
            List<String> parsed = new ArrayList<>(lore.size());
            for (String line : lore) {
                parsed.add(color(interpolate(line, data, index)));
            }
            meta.setLore(parsed);
        }
        if (resolved.glow()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private InventoryItemDefinition defaultTemplate(Object data) {
        if (data instanceof MultiblockDefinition) {
            return new InventoryItemDefinition(Material.PAPER, "&b{id}", List.of("&7Bloques: &f{blocks}"), false);
        }
        return new InventoryItemDefinition(Material.STONE, "&f{value}", List.of(), false);
    }

    private String interpolate(String text, Object data, int index) {
        String source = text == null ? "" : text;
        String out = source.replace("{index}", String.valueOf(index + 1));
        if (data instanceof MultiblockDefinition definition) {
            out = out.replace("{id}", definition.id() == null ? "" : definition.id());
            out = out.replace("{blocks}", String.valueOf(definition.blocks() == null ? 0 : definition.blocks().size()));
            return out;
        }
        return out.replace("{value}", String.valueOf(data));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public record RenderResult(Inventory inventory, Map<Integer, Object> bindings) {
    }

    public record ViewHolder(String viewId) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
