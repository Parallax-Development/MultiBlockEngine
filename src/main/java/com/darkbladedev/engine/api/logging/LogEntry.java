package com.darkbladedev.engine.api.logging;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record LogEntry(
    Instant timestamp,
    String engine,
    LogScope scope,
    LogPhase phase,
    LogLevel level,
    String message,
    List<LogKv> fields,
    Set<String> tags,
    Throwable throwable,
    boolean includeStacktrace
) {
}

