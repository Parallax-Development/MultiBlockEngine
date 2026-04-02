package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;

public record SetStateAction(MultiblockState newState) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        MultiBlockEngine.getInstance().getManager().updateInstanceState(instance, newState);
    }
}
