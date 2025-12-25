package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;

public record SetStateAction(MultiblockState newState) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        MultiBlockEngine.getInstance().getManager().updateInstanceState(instance, newState);
    }
}
