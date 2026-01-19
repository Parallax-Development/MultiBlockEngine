package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MatchResult;
import org.bukkit.block.Block;

public record AirMatcher() implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return block.getType().isAir();
    }

    @Override
    public MatchResult match(Block block) {
        if (block == null) {
            return MatchResult.fail("block is null");
        }
        return block.getType().isAir() ? MatchResult.ok() : MatchResult.fail("expected air");
    }
}
