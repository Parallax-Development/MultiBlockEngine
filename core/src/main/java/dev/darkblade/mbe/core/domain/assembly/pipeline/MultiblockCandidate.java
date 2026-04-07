package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.block.Block;

public record MultiblockCandidate(
        MultiblockType type,
        Block controller,
        String triggerId,
        boolean forceTrigger
) {
}
