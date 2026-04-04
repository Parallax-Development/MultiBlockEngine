package dev.darkblade.mbe.blueprint;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryBuildContextService implements BuildContextService {
    private final Map<UUID, PlayerBuildContext> contexts = new ConcurrentHashMap<>();

    @Override
    public PlayerBuildContext get(Player player) {
        if (player == null) {
            return new PlayerBuildContext();
        }
        return contexts.computeIfAbsent(player.getUniqueId(), id -> new PlayerBuildContext());
    }

    @Override
    public void setMode(Player player, Mode mode) {
        if (player == null) {
            return;
        }
        get(player).mode(mode);
    }

    @Override
    public void clear(Player player) {
        if (player == null) {
            return;
        }
        contexts.remove(player.getUniqueId());
    }
}
