package dev.darkblade.mbe.core.infrastructure.config.parser.item;

import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.core.infrastructure.config.item.YamlItemConfig;
import dev.darkblade.mbe.core.infrastructure.config.item.YamlItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemConfigParser {
    private final CoreLogger log;

    public ItemConfigParser(CoreLogger log) {
        this.log = log;
    }

    public Map<ItemKey, YamlItemDefinition> parse(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return Map.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("items");
        if (root == null) {
            warn("No items section found in items.yml", LogKv.kv("file", file.getAbsolutePath()));
            return Map.of();
        }
        Map<ItemKey, YamlItemDefinition> out = new LinkedHashMap<>();
        for (String entryId : root.getKeys(false)) {
            if (entryId == null || entryId.isBlank()) {
                continue;
            }
            try {
                ConfigurationSection section = root.getConfigurationSection(entryId);
                if (section == null) {
                    warn("Item entry is not a section", LogKv.kv("entry", entryId));
                    continue;
                }
                String key = section.getString("key", entryId);
                int version = Math.max(0, section.getInt("version", 0));
                String material = section.getString("material", "");
                String displayName = section.getString("display-name", "");
                List<String> lore = section.getStringList("lore");
                Integer customModelData = section.contains("custom-model-data") ? Integer.valueOf(section.getInt("custom-model-data")) : null;
                List<String> flags = section.getStringList("flags");
                boolean unstackable = section.getBoolean("unstackable", false);

                YamlItemConfig config = new YamlItemConfig(
                        key,
                        version,
                        material,
                        displayName,
                        lore,
                        customModelData,
                        flags,
                        unstackable
                );
                YamlItemDefinition definition = new YamlItemDefinition(config);
                if (out.containsKey(definition.key())) {
                    warn("Duplicate item key detected; entry ignored", LogKv.kv("entry", entryId), LogKv.kv("key", definition.key().id().toString()));
                    continue;
                }
                out.put(definition.key(), definition);
            } catch (Exception ex) {
                warn("Failed to parse item entry", LogKv.kv("entry", entryId), LogKv.kv("reason", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
        }
        return Map.copyOf(out);
    }

    private void warn(String message, LogKv... fields) {
        if (log == null) {
            return;
        }
        log.warn(message, fields);
    }
}
