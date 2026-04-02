package dev.darkblade.mbe.core.domain.condition;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.entity.Player;

public record PlayerSneakingCondition(boolean required) implements Condition {
    @Override
    public boolean check(MultiblockInstance instance, Player player) {
        if (player == null) return false;
        return player.isSneaking() == required;
    }
}
