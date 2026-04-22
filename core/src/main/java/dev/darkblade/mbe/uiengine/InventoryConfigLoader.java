package dev.darkblade.mbe.uiengine;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class InventoryConfigLoader {
    private final File file;

    public InventoryConfigLoader(File file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public Map<String, InventoryViewDefinition> load() {
        if (!file.exists()) {
            return Map.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection viewsSection = yaml.getConfigurationSection("views");
        if (viewsSection == null) {
            return Map.of();
        }
        Map<String, InventoryViewDefinition> out = new LinkedHashMap<>();
        for (String viewId : viewsSection.getKeys(false)) {
            ConfigurationSection section = viewsSection.getConfigurationSection(viewId);
            if (section == null) {
                continue;
            }
            InventoryViewDefinition definition = parseView(viewId, section);
            if (definition.id().isBlank()) {
                continue;
            }
            out.put(definition.id(), definition);
        }
        return Map.copyOf(out);
    }

    private InventoryViewDefinition parseView(String viewId, ConfigurationSection section) {
        String title = section.getString("title", viewId);
        int size = section.getInt("size", 54);
        List<String> layout = section.getStringList("layout");
        Map<Character, InventoryItemDefinition> items = parseItems(section.getConfigurationSection("items"));
        List<DynamicSection> dynamicSections = parseDynamicSections(section.getConfigurationSection("dynamic_sections"));
        Map<Integer, SlotRole> slotRoles = parseSlotRoles(section.getConfigurationSection("slot_roles"));
        return new InventoryViewDefinition(viewId, title, size, layout, items, dynamicSections, slotRoles);
    }

    private Map<Character, InventoryItemDefinition> parseItems(ConfigurationSection itemsSection) {
        if (itemsSection == null) {
            return Map.of();
        }
        Map<Character, InventoryItemDefinition> out = new LinkedHashMap<>();
        for (String key : itemsSection.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }
            ConfigurationSection row = itemsSection.getConfigurationSection(key);
            if (row == null) {
                continue;
            }
            Material material = Material.matchMaterial(row.getString("material", "STONE"));
            String name = row.getString("name", "");
            List<String> lore = row.getStringList("lore");
            boolean glow = row.getBoolean("glow", false);
            out.put(key.charAt(0), new InventoryItemDefinition(material, name, lore, glow));
        }
        return Map.copyOf(out);
    }

    private List<DynamicSection> parseDynamicSections(ConfigurationSection dynamicSection) {
        if (dynamicSection == null) {
            return List.of();
        }
        List<DynamicSection> out = new ArrayList<>();
        for (String key : dynamicSection.getKeys(false)) {
            ConfigurationSection row = dynamicSection.getConfigurationSection(key);
            if (row == null) {
                continue;
            }
            String symbolRaw = row.getString("symbol", "");
            if (symbolRaw.isBlank()) {
                continue;
            }
            String providerId = row.getString("provider", "").trim().toLowerCase(Locale.ROOT);
            if (providerId.isBlank()) {
                continue;
            }
            out.add(new DynamicSection(symbolRaw.charAt(0), providerId));
        }
        return List.copyOf(out);
    }

    private Map<Integer, SlotRole> parseSlotRoles(ConfigurationSection slotRolesSection) {
        if (slotRolesSection == null) {
            return Map.of();
        }
        Map<Integer, SlotRole> out = new LinkedHashMap<>();
        for (String key : slotRolesSection.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            String roleName = slotRolesSection.getString(key, "").trim().toUpperCase(Locale.ROOT);
            if (roleName.isBlank()) {
                continue;
            }
            try {
                out.put(slot, SlotRole.valueOf(roleName));
            } catch (IllegalArgumentException e) {
                // Unknown role — skip silently to allow forward compatibility
            }
        }
        return Map.copyOf(out);
    }
}

