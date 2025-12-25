package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import org.bukkit.block.Block;
import java.util.List;

public record AnyOfMatcher(List<BlockMatcher> matchers) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        for (BlockMatcher matcher : matchers) {
            if (matcher.matches(block)) return true;
        }
        return false;
    }
}
