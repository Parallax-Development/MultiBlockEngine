package dev.darkblade.mbe.preview;

import org.bukkit.Location;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PreviewSession {
    private final UUID playerId;
    private volatile MultiblockDefinition definition;
    private final Map<BlockPosition, SessionPreviewBlock> blocks;
    private final AtomicLong renderVersion;
    private volatile Location origin;
    private volatile Rotation rotation;
    private volatile PreviewState state;
    private volatile Instant lastTouchedAt;

    public PreviewSession(UUID playerId, MultiblockDefinition definition, Location origin, Rotation rotation) {
        this.playerId = playerId;
        this.definition = definition;
        this.origin = origin;
        this.rotation = rotation == null ? Rotation.NORTH : rotation;
        this.state = PreviewState.MOVING;
        this.blocks = new ConcurrentHashMap<>();
        this.renderVersion = new AtomicLong(0L);
        this.lastTouchedAt = Instant.now();
    }

    public UUID playerId() {
        return playerId;
    }

    public MultiblockDefinition definition() {
        return definition;
    }

    public void definition(MultiblockDefinition definition) {
        this.definition = definition;
        touch();
    }

    public Location origin() {
        return origin;
    }

    public void origin(Location origin) {
        this.origin = origin;
        touch();
    }

    public Rotation rotation() {
        return rotation;
    }

    public void rotation(Rotation rotation) {
        this.rotation = rotation == null ? Rotation.NORTH : rotation;
        touch();
    }

    public PreviewState state() {
        return state;
    }

    public void state(PreviewState state) {
        this.state = state == null ? PreviewState.MOVING : state;
        touch();
    }

    public Map<BlockPosition, SessionPreviewBlock> blocks() {
        return blocks;
    }

    public void clearBlocks() {
        this.blocks.clear();
    }

    public void trackBlock(BlockPosition position, SessionPreviewBlock block) {
        if (position == null || block == null) {
            return;
        }
        SessionPreviewBlock previous = this.blocks.get(position);
        if (previous != null && previous.completed()) {
            block.completed(true);
        }
        this.blocks.put(position, block);
    }

    public boolean markCompleted(BlockPosition position) {
        if (position == null) {
            return false;
        }
        SessionPreviewBlock block = this.blocks.get(position);
        if (block == null || block.completed()) {
            return false;
        }
        block.completed(true);
        return true;
    }

    public boolean isCompleted() {
        if (this.blocks.isEmpty()) {
            return false;
        }
        for (SessionPreviewBlock block : this.blocks.values()) {
            if (block == null || !block.completed()) {
                return false;
            }
        }
        return true;
    }

    public long currentRenderVersion() {
        return this.renderVersion.get();
    }

    public long nextRenderVersion() {
        return this.renderVersion.incrementAndGet();
    }

    public Instant lastTouchedAt() {
        return lastTouchedAt;
    }

    public void touch() {
        this.lastTouchedAt = Instant.now();
    }
}
