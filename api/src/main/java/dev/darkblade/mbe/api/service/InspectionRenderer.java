package dev.darkblade.mbe.api.service;

import org.bukkit.entity.Player;

public interface InspectionRenderer {
    void render(Player player, InspectionData data, InspectionContext ctx);
}

