package dev.darkblade.mbe.uiengine;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventorySessionStore {
    private final Map<UUID, PlayerInventorySession> sessions = new ConcurrentHashMap<>();

    public void put(Player player, PlayerInventorySession session) {
        if (player == null || session == null) {
            return;
        }
        sessions.put(player.getUniqueId(), session);
    }

    public Optional<PlayerInventorySession> get(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        sessions.remove(player.getUniqueId());
    }
}
