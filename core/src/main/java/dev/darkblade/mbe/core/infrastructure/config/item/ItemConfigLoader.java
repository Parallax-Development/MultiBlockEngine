package dev.darkblade.mbe.core.infrastructure.config.item;

import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.core.infrastructure.config.parser.item.ItemConfigParser;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemConfigLoader {
    private final File file;
    private final ItemConfigParser parser;
    private final CoreLogger log;

    public ItemConfigLoader(File file, ItemConfigParser parser, CoreLogger log) {
        this.file = file;
        this.parser = parser;
        this.log = log;
    }

    public Map<ItemKey, ItemDefinition> load() {
        ensureFileExists();
        Map<ItemKey, YamlItemDefinition> parsed = parser.parse(file);
        Map<ItemKey, ItemDefinition> out = new LinkedHashMap<>();
        for (Map.Entry<ItemKey, YamlItemDefinition> entry : parsed.entrySet()) {
            out.put(entry.getKey(), entry.getValue());
        }
        info("Configured items loaded", LogKv.kv("count", out.size()));
        return Map.copyOf(out);
    }

    private void ensureFileExists() {
        if (file == null || file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException ex) {
            warn("Failed to create items.yml file", LogKv.kv("file", file.getAbsolutePath()), LogKv.kv("reason", ex.getClass().getSimpleName()));
        }
    }

    private void info(String message, LogKv... fields) {
        if (log == null) {
            return;
        }
        log.info(message, fields);
    }

    private void warn(String message, LogKv... fields) {
        if (log == null) {
            return;
        }
        log.warn(message, fields);
    }
}
