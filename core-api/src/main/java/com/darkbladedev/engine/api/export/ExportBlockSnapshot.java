package com.darkbladedev.engine.api.export;

import org.bukkit.Material;

public record ExportBlockSnapshot(
        ExportBlockPos pos,
        Material material,
        String blockData
) {
}

