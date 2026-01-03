package com.mbe.ui.api.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface MenuClickContext {
    PlayerContext player();

    ClickType clickType();

    int slot();

    ItemStack cursorItem();

    void cursorItem(ItemStack stack);

    ItemStack slotItem();

    void slotItem(ItemStack stack);

    void refresh();

    void close();
}

