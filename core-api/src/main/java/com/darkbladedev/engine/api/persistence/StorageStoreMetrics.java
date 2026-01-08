package com.darkbladedev.engine.api.persistence;

public record StorageStoreMetrics(
    long pendingWrites,
    long totalWrites,
    long totalBytesWritten,
    long lastWriteTimestamp
) {
}
