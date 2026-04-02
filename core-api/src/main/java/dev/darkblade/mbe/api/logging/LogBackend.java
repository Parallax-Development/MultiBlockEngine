package dev.darkblade.mbe.api.logging;

public interface LogBackend {
    void publish(LogEntry entry);
}

