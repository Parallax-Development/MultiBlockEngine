package dev.darkblade.mbe.uiengine.blueprint;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores per-player {@link BlueprintCraftingSession} instances for the duration
 * that a player has the Blueprint Crafting Table panel open.
 * <p>
 * Sessions must be explicitly removed when the panel is closed (via
 * {@code InventoryCloseEvent}) to avoid memory leaks.
 */
public final class BlueprintCraftingSessionStore {

    private final Map<UUID, BlueprintCraftingSession> sessions = new ConcurrentHashMap<>();

    public void put(Player player, BlueprintCraftingSession session) {
        if (player == null || session == null) return;
        sessions.put(player.getUniqueId(), session);
    }

    public Optional<BlueprintCraftingSession> get(Player player) {
        if (player == null) return Optional.empty();
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public boolean has(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public void remove(Player player) {
        if (player == null) return;
        sessions.remove(player.getUniqueId());
    }
}
