package dev.darkblade.mbe.api.service;

import org.bukkit.entity.Player;

public interface InspectionPipelineService {

    default void inspect(Player player, InteractionSource source, Inspectable inspectable, InspectionRenderer renderer) {
        inspect(player, source, null, inspectable, renderer);
    }

    void inspect(Player player, InteractionSource source, InspectionLevel requestedLevel, Inspectable inspectable, InspectionRenderer renderer);
}

