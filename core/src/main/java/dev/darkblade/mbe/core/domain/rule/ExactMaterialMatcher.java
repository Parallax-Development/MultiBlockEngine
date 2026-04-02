package dev.darkblade.mbe.core.domain.rule;

import dev.darkblade.mbe.core.domain.BlockMatcher;
import org.bukkit.Material;
import org.bukkit.block.Block;

public record ExactMaterialMatcher(Material material) implements BlockMatcher {
    @Override
    public boolean matches(Block block) {
        return block.getType() == material;
    }
}
