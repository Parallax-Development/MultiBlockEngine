package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.condition.Condition;
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
