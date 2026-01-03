package com.mbe.ui.api.menu;

import org.bukkit.entity.Player;

public interface MenuController {
    void open(UIMenu menu, Player player);

    void refresh(Player player);

    void close(Player player);
}

