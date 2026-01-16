package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import org.bukkit.block.Block;

public record AirMatcher() implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return block.getType().isAir();
    }
}
