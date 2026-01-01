package com.darkbladedev.engine.api.logging;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class LoggingConfig {

    private final LogLevel level;
    private final boolean debug;
    private final boolean debugCore;
    private final boolean debugAddons;
    private final Set<String> debugAddonsById;

    public LoggingConfig(LogLevel level, boolean debug, boolean debugCore, boolean debugAddons, Set<String> debugAddonsById) {
        this.level = Objects.requireNonNull(level, "level");
        this.debug = debug;
        this.debugCore = debugCore;
        this.debugAddons = debugAddons;
        this.debugAddonsById = debugAddonsById == null ? Set.of() : Set.copyOf(debugAddonsById);
    }

    public LogLevel level() {
        return level;
    }

    public boolean isDebug(LogScope scope) {
        if (debug) {
            return true;
        }

        if (scope instanceof LogScope.Core) {
            return debugCore;
        }

        if (scope instanceof LogScope.Addon addon) {
            if (debugAddons) {
                return true;
            }
            String id = addon.addonId();
            String key = id == null ? "" : id.toLowerCase(Locale.ROOT);
            return debugAddonsById.contains(key);
        }

        return false;
    }

    public boolean isEnabled(LogScope scope, LogLevel eventLevel) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(eventLevel, "eventLevel");

        LogLevel threshold = isDebug(scope) ? LogLevel.TRACE : level;
        return eventLevel.ordinal() >= threshold.ordinal();
    }

    public static Set<String> normalizeIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new HashSet<>();
        for (String id : ids) {
            if (id == null) continue;
            String t = id.trim();
            if (!t.isEmpty()) {
                out.add(t.toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(out);
    }
}

