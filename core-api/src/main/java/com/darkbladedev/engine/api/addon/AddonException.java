package com.darkbladedev.engine.api.addon;

public class AddonException extends Exception {

    public enum Phase {
        LOAD,
        ENABLE,
        DISABLE,
        RUNTIME
    }

    private final String addonId;
    private final boolean fatal;
    private final Phase phase;
    private final String context;

    public AddonException(String addonId, String message) {
        this(addonId, message, false, null, null);
    }

    public AddonException(String addonId, String message, boolean fatal) {
        this(addonId, message, fatal, null, null);
    }

    public AddonException(String addonId, String message, Throwable cause) {
        this(addonId, message, cause, false, null, null);
    }

    public AddonException(String addonId, String message, Throwable cause, boolean fatal) {
        this(addonId, message, cause, fatal, null, null);
    }

    public AddonException(String addonId, String message, boolean fatal, Phase phase, String context) {
        super(message);
        this.addonId = addonId;
        this.fatal = fatal;
        this.phase = phase;
        this.context = context;
    }

    public AddonException(String addonId, String message, Throwable cause, boolean fatal, Phase phase, String context) {
        super(message, cause);
        this.addonId = addonId;
        this.fatal = fatal;
        this.phase = phase;
        this.context = context;
    }

    public String getAddonId() {
        return addonId;
    }

    public boolean isFatal() {
        return fatal;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getContext() {
        return context;
    }
}
