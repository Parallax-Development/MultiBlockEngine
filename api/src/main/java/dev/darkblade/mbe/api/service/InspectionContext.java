package dev.darkblade.mbe.api.service;

import org.bukkit.entity.Player;

public record InspectionContext(
    Player player,
    InspectionLevel requestedLevel,
    InteractionSource source
) {
}

