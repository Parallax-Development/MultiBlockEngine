package com.darkbladedev.engine.api.logging;

import java.util.Objects;
import java.util.Set;

public final class AddonLogger implements EngineLogger {

    private final CoreLogger core;
    private final String addonId;
    private final String addonVersion;
    private final AddonPhaseProvider phaseProvider;

    public AddonLogger(CoreLogger core, String addonId, String addonVersion, AddonPhaseProvider phaseProvider) {
        this.core = Objects.requireNonNull(core, "core");
        this.addonId = Objects.requireNonNull(addonId, "addonId");
        this.addonVersion = Objects.requireNonNull(addonVersion, "addonVersion");
        this.phaseProvider = Objects.requireNonNull(phaseProvider, "phaseProvider");
    }

    public EngineLogger withPhase(LogPhase phase) {
        Objects.requireNonNull(phase, "phase");
        return (level, message, throwable, fields) -> core.logInternal(new LogScope.Addon(addonId, addonVersion), phase, level, message, throwable, fields, Set.of());
    }

    @Override
    public void log(LogLevel level, String message, Throwable throwable, LogKv... fields) {
        core.logInternal(new LogScope.Addon(addonId, addonVersion), phaseProvider.currentPhase(), level, message, throwable, fields, Set.of());
    }
}

