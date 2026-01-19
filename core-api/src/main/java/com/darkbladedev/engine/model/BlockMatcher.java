package com.darkbladedev.engine.model;

import org.bukkit.block.Block;

public interface BlockMatcher {
    boolean matches(Block block);

    default MatchResult match(Block block) {
        return matches(block) ? MatchResult.ok() : MatchResult.fail("mismatch");
    }
}
