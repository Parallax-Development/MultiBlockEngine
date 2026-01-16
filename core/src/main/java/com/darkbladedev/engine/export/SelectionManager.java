package com.darkbladedev.engine.export;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionManager {

    private final Map<UUID, ExportSession> sessions = new ConcurrentHashMap<>();

    public ExportSession start(Player player) {
        UUID id = player == null ? null : player.getUniqueId();
        if (id == null) {
            throw new IllegalArgumentException("player");
        }
        ExportSession s = new ExportSession(id);
        sessions.put(id, s);
        return s;
    }

    public ExportSession session(Player player) {
        UUID id = player == null ? null : player.getUniqueId();
        if (id == null) {
            return null;
        }
        return sessions.get(id);
    }

    public ExportSession session(UUID playerId) {
        return playerId == null ? null : sessions.get(playerId);
    }

    public boolean cancel(Player player) {
        UUID id = player == null ? null : player.getUniqueId();
        if (id == null) {
            return false;
        }
        return sessions.remove(id) != null;
    }
}

