package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.preview.PreviewSession;
import org.bukkit.Location;

public final class PlayerBuildContext {
    private volatile Mode mode;
    private volatile PreviewSession preview;
    private volatile String blueprintId;
    private volatile Location lastResolvedOrigin;

    public PlayerBuildContext() {
        this.mode = Mode.NONE;
    }

    public Mode mode() {
        return mode;
    }

    public void mode(Mode mode) {
        this.mode = mode == null ? Mode.NONE : mode;
    }

    public PreviewSession preview() {
        return preview;
    }

    public void preview(PreviewSession preview) {
        this.preview = preview;
    }

    public String blueprintId() {
        return blueprintId;
    }

    public void blueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Location lastResolvedOrigin() {
        return lastResolvedOrigin;
    }

    public void lastResolvedOrigin(Location lastResolvedOrigin) {
        this.lastResolvedOrigin = lastResolvedOrigin;
    }
}
