package com.mbe.ui.api.menu;

public interface UIMenu {
    MenuId id();

    default String title(PlayerContext ctx) {
        return id().name();
    }

    int size();

    MenuView render(PlayerContext ctx);
}

