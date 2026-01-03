package com.mbe.ui.api.menu;

import org.bukkit.inventory.ItemStack;

public interface MenuItem {
    ItemStack render(PlayerContext ctx);

    void onClick(MenuClickContext ctx);
}

