package com.darkbladedev.engine.api.persistence;

public interface StorageSchema {

    int schemaVersion();

    StorageSchemaMigrator migrator();

    interface StorageSchemaMigrator {

        byte[] migrate(int fromVersion, int toVersion, byte[] payload);
    }
}
