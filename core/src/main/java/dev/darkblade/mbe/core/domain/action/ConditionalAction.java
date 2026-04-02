package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.condition.Condition;
import org.bukkit.entity.Player;

import java.util.List;

public record ConditionalAction(List<Condition> conditions, List<Action> thenActions, List<Action> elseActions) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        boolean met = true;
        for (Condition c : conditions) {
            if (!c.check(instance, player)) {
                met = false;
                break;
            }
        }
        
        List<Action> toRun = met ? thenActions : elseActions;
        for (Action action : toRun) {
            action.execute(instance, player);
        }
    }
}
