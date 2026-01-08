package com.darkbladedev.engine.api.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StorageStore {

    String id();

    StorageSchema schema();

    Optional<StoredRecord> read(String key);

    Map<String, StoredRecord> readAll();

    StorageWriteResult write(String key, byte[] payload, StorageRecordMeta meta);

    StorageWriteResult delete(String key, StorageRecordMeta meta);

    CompletableFuture<StorageWriteResult> writeAsync(String key, byte[] payload, StorageRecordMeta meta);

    CompletableFuture<StorageWriteResult> deleteAsync(String key, StorageRecordMeta meta);

    StorageStoreMetrics metrics();
}
