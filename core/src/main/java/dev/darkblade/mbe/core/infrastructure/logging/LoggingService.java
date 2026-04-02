package dev.darkblade.mbe.core.infrastructure.logging;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LoggingConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LoggingService {

    private final CoreLogger core;

    public LoggingService(MultiBlockEngine plugin) {
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
