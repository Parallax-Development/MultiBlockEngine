package dev.darkblade.mbe.platform.bukkit.preview.api;

import java.util.UUID;

public record DisplayEntityHandle(int entityId, UUID uuid) {
    public boolean isValid() {
        return entityId > 0 && uuid != null;
    }
}
