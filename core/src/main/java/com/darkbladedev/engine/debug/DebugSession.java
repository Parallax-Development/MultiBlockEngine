package com.darkbladedev.engine.debug;

import com.darkbladedev.engine.model.MultiblockType;
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
