package dev.darkblade.mbe.core.infrastructure.config.item;

import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class YamlItemDefinition implements ItemDefinition {
    private final ItemKey key;
    private final String displayName;
    private final Map<String, Object> properties;

    public YamlItemDefinition(YamlItemConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Item config cannot be null");
        }
        config.validateRequired();
        this.key = ItemKeys.of(resolveNamespacedId(config.key()), config.version());
        this.displayName = config.displayName();
        String normalizedMaterial = normalizeMaterial(config.material());
        List<String> normalizedFlags = normalizeFlags(config.flags());
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("material", normalizedMaterial);
        props.put("lore", config.lore());
        if (config.customModelData() != null) {
            props.put("custom-model-data", Math.max(0, config.customModelData()));
        }
        if (!normalizedFlags.isEmpty()) {
            props.put("flags", normalizedFlags);
        }
        if (config.unstackable()) {
            props.put("unstackable", true);
        }
        this.properties = Map.copyOf(props);
    }

    @Override
    public ItemKey key() {
        return key;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Map<String, Object> properties() {
        return properties;
    }

    private static String resolveNamespacedId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Item key cannot be blank");
        }
        if (value.contains(":")) {
            return value;
        }
        return "mbe:" + value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeMaterial(String rawMaterial) {
        String candidate = rawMaterial.trim().toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(candidate);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material: " + rawMaterial);
        }
        return material.name();
    }

    private static List<String> normalizeFlags(List<String> rawFlags) {
        if (rawFlags == null || rawFlags.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (String raw : rawFlags) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String candidate = raw.trim().toUpperCase(Locale.ROOT);
            ItemFlag.valueOf(candidate);
            out.add(candidate);
        }
        return List.copyOf(out);
    }
}
