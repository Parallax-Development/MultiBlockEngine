package dev.darkblade.mbe.api.persistence;

public record StorageMetrics(
    long pendingWrites,
    long totalWrites,
    long totalBytesWritten,
    long lastFlushTimestamp,
    long recoveryActions
) {
}
