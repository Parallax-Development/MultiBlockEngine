package com.darkbladedev.engine.api.persistence;

import java.util.Objects;

public record StorageRecordMeta(
    String producerId,
    long timestamp
) {
    public StorageRecordMeta {
        producerId = Objects.requireNonNull(producerId, "producerId");
    }

    public static StorageRecordMeta now(String producerId) {
        return new StorageRecordMeta(producerId, System.currentTimeMillis());
    }
}
