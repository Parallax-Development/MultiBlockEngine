package com.darkbladedev.engine.api.persistence;

public record StorageMetrics(
    long pendingWrites,
    long totalWrites,
    long totalBytesWritten,
    long lastFlushTimestamp,
    long recoveryActions
) {
}
