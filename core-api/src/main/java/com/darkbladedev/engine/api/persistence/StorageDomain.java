package com.darkbladedev.engine.api.persistence;

public interface StorageDomain {

    String id();

    StorageStore store(String storeId, StorageSchema schema);
}
