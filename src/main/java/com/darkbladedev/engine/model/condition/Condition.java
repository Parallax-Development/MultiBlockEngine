package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;

public interface Condition {
    boolean check(MultiblockInstance instance, Player player);
}
