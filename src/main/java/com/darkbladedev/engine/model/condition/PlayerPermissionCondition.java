package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;

public record PlayerPermissionCondition(String permission) implements Condition {
    @Override
    public boolean check(MultiblockInstance instance, Player player) {
        if (player == null) return false;
        return player.hasPermission(permission);
    }
}
