package dev.darkblade.mbe.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public class ComponentAvailabilityEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String addonId;
    private final String componentId;
    private final ComponentKind componentKind;
    private final ComponentChangeType changeType;

    public ComponentAvailabilityEvent(
            @NotNull String addonId,
            @NotNull String componentId,
            @NotNull ComponentKind componentKind,
            @NotNull ComponentChangeType changeType
    ) {
        this.addonId = normalize(Objects.requireNonNull(addonId, "addonId"));
        this.componentId = normalize(Objects.requireNonNull(componentId, "componentId"));
        this.componentKind = Objects.requireNonNull(componentKind, "componentKind");
        this.changeType = Objects.requireNonNull(changeType, "changeType");
    }

    @NotNull
    public String getAddonId() {
        return addonId;
    }

    @NotNull
    public String getComponentId() {
        return componentId;
    }

    @NotNull
    public ComponentKind getComponentKind() {
        return componentKind;
    }

    @NotNull
    public ComponentChangeType getChangeType() {
        return changeType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("blank");
        }
        return normalized;
    }
}
