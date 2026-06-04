package dev.darkblade.mbe.core.application.service.addon.domain;
import dev.darkblade.mbe.api.logging.LogLevel;
public record AddonLoggingSettings(boolean suppressNonCritical, LogLevel minLevelWhenSuppressed) {
    public AddonLoggingSettings {
        minLevelWhenSuppressed = minLevelWhenSuppressed == null ? LogLevel.ERROR : minLevelWhenSuppressed;
    }
}
