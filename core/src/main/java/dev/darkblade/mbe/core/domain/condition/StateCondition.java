package dev.darkblade.mbe.core.domain.condition;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;

public record StateCondition(MultiblockState requiredState) implements Condition {
    @Override
    public boolean check(MultiblockInstance instance) {
        return instance.state() == requiredState;
    }
}
