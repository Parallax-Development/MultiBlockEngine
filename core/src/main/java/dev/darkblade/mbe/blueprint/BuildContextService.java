package dev.darkblade.mbe.blueprint;

import org.bukkit.entity.Player;

public interface BuildContextService {
    PlayerBuildContext get(Player player);
    void setMode(Player player, Mode mode);
    void clear(Player player);
}
