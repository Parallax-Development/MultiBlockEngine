package dev.darkblade.mbe.api.ui.runtime;

import org.bukkit.entity.Player;

import java.util.Map;

@FunctionalInterface
public interface PanelAction {
    void execute(Player player, Map<String, Object> context);
}
