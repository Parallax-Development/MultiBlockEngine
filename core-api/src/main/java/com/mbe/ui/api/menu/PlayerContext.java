package com.mbe.ui.api.menu;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public interface PlayerContext {
    Player player();

    UUID uuid();

    Locale locale();

    Map<String, Object> sessionData();

    default Map<String, Object> session() {
        return sessionData();
    }
}

