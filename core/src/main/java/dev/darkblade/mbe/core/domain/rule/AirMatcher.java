package dev.darkblade.mbe.core.domain.rule;

import dev.darkblade.mbe.core.domain.BlockMatcher;
import org.bukkit.block.Block;

public record AirMatcher() implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return block.getType().isAir();
    }
}
