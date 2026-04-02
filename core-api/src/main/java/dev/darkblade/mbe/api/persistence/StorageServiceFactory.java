package dev.darkblade.mbe.api.persistence;

public interface StorageServiceFactory {

    StorageService create(StorageDescriptor descriptor);
}
