package dev.darkblade.mbe.core.infrastructure.config.item;

import java.util.List;
import java.util.Objects;

public final class YamlItemConfig {
    private final String key;
    private final int version;
    private final String material;
    private final String displayName;
    private final List<String> lore;
    private final Integer customModelData;
    private final List<String> flags;
    private final boolean unstackable;

    public YamlItemConfig(
            String key,
            int version,
            String material,
            String displayName,
            List<String> lore,
            Integer customModelData,
            List<String> flags,
            boolean unstackable
    ) {
        this.key = key == null ? "" : key.trim();
        this.version = Math.max(0, version);
        this.material = material == null ? "" : material.trim();
        this.displayName = displayName == null ? "" : displayName;
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.customModelData = customModelData;
        this.flags = flags == null ? List.of() : List.copyOf(flags);
        this.unstackable = unstackable;
    }

    public String key() {
        return key;
    }

    public int version() {
        return version;
    }

    public String material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> lore() {
        return lore;
    }

    public Integer customModelData() {
        return customModelData;
    }

    public List<String> flags() {
        return flags;
    }

    public boolean unstackable() {
        return unstackable;
    }

    public void validateRequired() {
        if (key.isBlank()) {
            throw new IllegalArgumentException("Item key cannot be blank");
        }
        if (material.isBlank()) {
            throw new IllegalArgumentException("Item material cannot be blank for key " + key);
        }
        Objects.requireNonNull(lore, "lore");
        Objects.requireNonNull(flags, "flags");
    }
}
