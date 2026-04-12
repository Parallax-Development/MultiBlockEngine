package dev.darkblade.mbe.api.ui.runtime;

import org.bukkit.entity.Player;

public interface PanelOpenService {
    boolean openPanel(Player player, String panelId);
}
