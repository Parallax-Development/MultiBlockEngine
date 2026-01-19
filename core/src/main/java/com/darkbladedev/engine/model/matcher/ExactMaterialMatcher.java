package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MatchResult;
import org.bukkit.Material;
import org.bukkit.block.Block;

public record ExactMaterialMatcher(Material material) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return block.getType() == material;
    }

    @Override
    public MatchResult match(Block block) {
        if (block == null) {
            return MatchResult.fail("block is null");
        }
        if (material == null) {
            return MatchResult.fail("material is null");
        }
        return block.getType() == material ? MatchResult.ok() : MatchResult.fail("material mismatch");
    }
}
