package com.darkbladedev.engine.api.persistence;

public record StoredRecord(
    byte[] payload,
    int schemaVersion,
    String producerId,
    long timestamp,
    int crc32
) {
}
