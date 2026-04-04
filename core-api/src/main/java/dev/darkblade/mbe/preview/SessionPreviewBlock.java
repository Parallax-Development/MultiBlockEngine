package dev.darkblade.mbe.preview;

import org.bukkit.block.data.BlockData;

public final class SessionPreviewBlock {
    private final BlockData expected;
    private final int entityId;
    private volatile boolean completed;

    public SessionPreviewBlock(BlockData expected, int entityId, boolean completed) {
        this.expected = expected;
        this.entityId = entityId;
        this.completed = completed;
    }

    public BlockData expected() {
        return expected;
    }

    public int entityId() {
        return entityId;
    }

    public boolean completed() {
        return completed;
    }

    public void completed(boolean completed) {
        this.completed = completed;
    }
}
