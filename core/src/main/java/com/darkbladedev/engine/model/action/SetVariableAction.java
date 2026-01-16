package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;

public record SetVariableAction(String key, Object value) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        instance.setVariable(key, value);
    }
}
