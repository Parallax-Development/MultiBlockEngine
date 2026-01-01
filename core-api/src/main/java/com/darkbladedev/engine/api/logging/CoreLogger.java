package com.darkbladedev.engine.api.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class CoreLogger implements EngineLogger {

    private final String engine;
    private final LogBackend backend;
    private final LoggingConfig config;
    private final AtomicReference<LogPhase> corePhase = new AtomicReference<>(LogPhase.BOOT);

    public CoreLogger(String engine, LogBackend backend, LoggingConfig config) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.backend = Objects.requireNonNull(backend, "backend");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void setCorePhase(LogPhase phase) {
        corePhase.set(Objects.requireNonNull(phase, "phase"));
    }

    public AddonLogger forAddon(String addonId, String addonVersion, AddonPhaseProvider phaseProvider) {
        return new AddonLogger(this, addonId, addonVersion, phaseProvider);
    }

    @Override
    public void log(LogLevel level, String message, Throwable throwable, LogKv... fields) {
        logInternal(new LogScope.Core(), corePhase.get(), level, message, throwable, fields, Set.of());
    }

    public void logInternal(LogScope scope, LogPhase phase, LogLevel level, String message, Throwable throwable, LogKv[] fields, Set<String> tags) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(level, "level");

        if (!config.isEnabled(scope, level)) {
            return;
        }

        List<LogKv> list = new ArrayList<>();
        if (fields != null) {
            for (LogKv kv : fields) {
                if (kv != null) {
                    list.add(kv);
                }
            }
        }

        Set<String> tagSet = tags == null || tags.isEmpty() ? Set.of() : new LinkedHashSet<>(tags);

        boolean includeStacktrace = throwable != null && config.isDebug(scope);

        backend.publish(new LogEntry(
            Instant.now(),
            engine,
            scope,
            phase,
            level,
            message == null ? "" : message,
            List.copyOf(list),
            Set.copyOf(tagSet),
            throwable,
            includeStacktrace
        ));
    }
}

