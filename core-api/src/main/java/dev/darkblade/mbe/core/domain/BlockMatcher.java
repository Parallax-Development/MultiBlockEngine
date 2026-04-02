package dev.darkblade.mbe.core.domain;

import org.bukkit.block.Block;

public interface BlockMatcher {
    boolean matches(Block block);
}
