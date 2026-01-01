package com.darkbladedev.engine.model;

import org.bukkit.util.Vector;

public record PatternEntry(Vector offset, BlockMatcher matcher, boolean optional) {
    public PatternEntry(Vector offset, BlockMatcher matcher) {
        this(offset, matcher, false);
    }
}
