package com.darkbladedev.engine.api.logging;

public interface LogBackend {
    void publish(LogEntry entry);
}

