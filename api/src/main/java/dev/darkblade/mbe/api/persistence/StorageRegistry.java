package dev.darkblade.mbe.api.persistence;

public interface StorageRegistry {

    void registerFactory(String type, StorageServiceFactory factory);

    StorageService create(String type, StorageDescriptor descriptor);
}
