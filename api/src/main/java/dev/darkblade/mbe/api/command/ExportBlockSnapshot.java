package dev.darkblade.mbe.api.command;

import org.bukkit.Material;

public record ExportBlockSnapshot(
        ExportBlockPos pos,
        Material material,
        String blockData
) {
}

