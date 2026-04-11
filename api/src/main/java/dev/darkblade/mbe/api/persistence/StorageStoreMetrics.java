package dev.darkblade.mbe.api.persistence;

public record StorageStoreMetrics(
    long pendingWrites,
    long totalWrites,
    long totalBytesWritten,
    long lastWriteTimestamp
) {
}
