package com.darkbladedev.engine.api.logging;

@FunctionalInterface
public interface AddonPhaseProvider {
    LogPhase currentPhase();
}

