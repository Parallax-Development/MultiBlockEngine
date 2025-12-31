package com.darkbladedev.engine.logging;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LoggingConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LoggingManager {

    private final CoreLogger core;

    public LoggingManager(MultiBlockEngine plugin) {
        LoggingConfig config = readConfig(plugin);
        this.core = new CoreLogger("MultiBlockEngine", new JulConsoleBackend(plugin.getLogger()), config);
    }

    public CoreLogger core() {
        return core;
    }

    public void setCorePhase(LogPhase phase) {
        core.setCorePhase(phase);
    }

    private static LoggingConfig readConfig(MultiBlockEngine plugin) {
        String levelStr = plugin.getConfig().getString("logging.level", "INFO");
        LogLevel level = parseLevel(levelStr, LogLevel.INFO);

        boolean debug = plugin.getConfig().getBoolean("logging.debug", false);
        boolean debugCore = plugin.getConfig().getBoolean("logging.debugCore", false);
        boolean debugAddons = plugin.getConfig().getBoolean("logging.debugAddons", false);
        List<String> ids = plugin.getConfig().getStringList("logging.debugAddonsById");
        Set<String> normalized = LoggingConfig.normalizeIds(ids == null ? Set.of() : new HashSet<>(ids));

        return new LoggingConfig(level, debug, debugCore, debugAddons, normalized);
    }

    private static LogLevel parseLevel(String raw, LogLevel fallback) {
        if (raw == null) {
            return fallback;
        }
        String t = raw.trim().toUpperCase();
        if (t.isEmpty()) {
            return fallback;
        }
        try {
            return LogLevel.valueOf(t);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
