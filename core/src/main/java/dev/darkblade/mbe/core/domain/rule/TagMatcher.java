package dev.darkblade.mbe.core.domain.rule;

import dev.darkblade.mbe.core.domain.BlockMatcher;
import org.bukkit.Tag;
import org.bukkit.Material;
import org.bukkit.block.Block;

public record TagMatcher(Tag<Material> tag) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return tag.isTagged(block.getType());
    }
}
