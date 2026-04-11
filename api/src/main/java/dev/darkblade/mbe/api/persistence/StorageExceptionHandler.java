package dev.darkblade.mbe.api.persistence;

public interface StorageExceptionHandler {

    void handle(StorageService storage, Throwable error);
}
