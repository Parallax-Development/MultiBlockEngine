package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.core.domain.MultiblockInstance;

public record SetVariableAction(String key, Object value) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        instance.setVariable(key, value);
    }
}
