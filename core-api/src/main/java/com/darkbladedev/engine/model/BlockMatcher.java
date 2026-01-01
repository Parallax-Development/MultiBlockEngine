package com.darkbladedev.engine.model;

import org.bukkit.block.Block;

public interface BlockMatcher {
    boolean matches(Block block);
}
