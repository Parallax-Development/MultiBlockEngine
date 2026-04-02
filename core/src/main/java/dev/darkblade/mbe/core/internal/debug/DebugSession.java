package dev.darkblade.mbe.core.internal.debug;

import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.UUID;

public record DebugSession(
    UUID id,
    Player player,
    MultiblockType type,
    Location anchor,
    long startTime,
    long durationMillis
) {
    public boolean isExpired() {
        return System.currentTimeMillis() > startTime + durationMillis;
    }
}
