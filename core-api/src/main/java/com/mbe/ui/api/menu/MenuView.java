package com.mbe.ui.api.menu;

import java.util.Map;
import java.util.Optional;

public interface MenuView {
    Map<Integer, MenuItem> items();

    default Optional<MenuItem> filler() {
        return Optional.empty();
    }
}
