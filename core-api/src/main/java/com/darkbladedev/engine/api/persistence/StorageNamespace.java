package com.darkbladedev.engine.api.persistence;

public interface StorageNamespace {

    String id();

    StorageDomain domain(String domain);
}
