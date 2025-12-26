package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;

public interface Action {
    default void execute(MultiblockInstance instance) {
        execute(instance, null);
    }
    
    default void execute(MultiblockInstance instance, Player player) {
        execute(instance);
    }
}
