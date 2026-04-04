package dev.darkblade.mbe.api.ui;

import org.bukkit.entity.Player;

public interface PanelViewService {
    boolean panelExists(String panelId);
    void openPanel(Player player, String panelId);
}
