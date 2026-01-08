package com.darkbladedev.engine.api.persistence;

import java.util.List;

public record StorageFlushReport(
    long timestamp,
    List<StorageFlushResult> results
) {
    public StorageFlushReport {
        results = results == null ? List.of() : List.copyOf(results);
    }

    public record StorageFlushResult(
        String namespace,
        String domain,
        String store,
        boolean success,
        long bytesWritten,
        long durationMillis,
        String error
    ) {
    }
}
