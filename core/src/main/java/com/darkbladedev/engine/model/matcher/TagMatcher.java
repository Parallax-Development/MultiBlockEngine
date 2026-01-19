package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MatchResult;
import org.bukkit.Tag;
import org.bukkit.Material;
import org.bukkit.block.Block;

public record TagMatcher(Tag<Material> tag) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return tag.isTagged(block.getType());
    }

    @Override
    public MatchResult match(Block block) {
        if (block == null) {
            return MatchResult.fail("block is null");
        }
        if (tag == null) {
            return MatchResult.fail("tag is null");
        }
        return tag.isTagged(block.getType()) ? MatchResult.ok() : MatchResult.fail("tag mismatch");
    }
}
