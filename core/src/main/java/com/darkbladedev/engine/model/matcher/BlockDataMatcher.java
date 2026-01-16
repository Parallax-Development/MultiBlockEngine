package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public record BlockDataMatcher(BlockData expectedData) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return block.getBlockData().matches(expectedData);
    }
}
