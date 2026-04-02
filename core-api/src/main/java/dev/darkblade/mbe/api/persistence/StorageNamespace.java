package dev.darkblade.mbe.api.persistence;

public interface StorageNamespace {

    String id();

    StorageDomain domain(String domain);
}
