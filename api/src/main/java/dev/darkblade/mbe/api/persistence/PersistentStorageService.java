package dev.darkblade.mbe.api.persistence;

import java.util.concurrent.CompletableFuture;

public interface PersistentStorageService {

    void initialize();

    StorageRecoveryReport recover();

    StorageFlushReport flush();

    void shutdown(boolean graceful);

    StorageNamespace namespace(String namespace);

    StorageMetrics metrics();

    default AddonStorage forAddon(dev.darkblade.mbe.api.addon.AddonContext context) {
        return AddonStorage.from(this, context);
    }

    default AddonStorage forAddon(dev.darkblade.mbe.api.addon.AddonContext context, AddonStorage.Options options) {
        return AddonStorage.from(this, context, options);
    }

    default CompletableFuture<StorageFlushReport> flushAsync() {
        return CompletableFuture.supplyAsync(this::flush);
    }
}
