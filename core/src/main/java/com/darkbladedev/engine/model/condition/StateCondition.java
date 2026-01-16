package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;

public record StateCondition(MultiblockState requiredState) implements Condition {
    @Override
    public boolean check(MultiblockInstance instance) {
        return instance.state() == requiredState;
    }
}
