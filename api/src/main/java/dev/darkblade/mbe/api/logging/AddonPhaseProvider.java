package dev.darkblade.mbe.api.logging;

@FunctionalInterface
public interface AddonPhaseProvider {
    LogPhase currentPhase();
}

