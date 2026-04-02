package dev.darkblade.mbe.core.internal.debug;

public interface DebugRenderer {
    void start(DebugSession session);
    void stop(DebugSession session);
}
