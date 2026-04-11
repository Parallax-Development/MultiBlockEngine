package dev.darkblade.mbe.api.tool.event;

import dev.darkblade.mbe.api.command.WrenchContext;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ToolModePreExecuteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String toolId;
    private final String modeId;
    private final WrenchContext context;
    private boolean cancelled;

    public ToolModePreExecuteEvent(String toolId, String modeId, WrenchContext context) {
        this.toolId = Objects.requireNonNull(toolId, "toolId");
        this.modeId = Objects.requireNonNull(modeId, "modeId");
        this.context = Objects.requireNonNull(context, "context");
    }

    public String getToolId() {
        return toolId;
    }

    public String getModeId() {
        return modeId;
    }

    public WrenchContext getContext() {
        return context;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
