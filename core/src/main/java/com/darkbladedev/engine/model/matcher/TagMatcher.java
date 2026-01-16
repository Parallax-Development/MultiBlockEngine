package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import org.bukkit.Tag;
import org.bukkit.Material;
import org.bukkit.block.Block;

public record TagMatcher(Tag<Material> tag) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return tag.isTagged(block.getType());
    }
}
