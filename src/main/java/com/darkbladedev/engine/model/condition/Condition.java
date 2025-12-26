package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;

public interface Condition {
    // Default method for backward compatibility
    default boolean check(MultiblockInstance instance) {
        return check(instance, null);
    }

    // New method supporting player context
    default boolean check(MultiblockInstance instance, Player player) {
        return check(instance);
    }
}
