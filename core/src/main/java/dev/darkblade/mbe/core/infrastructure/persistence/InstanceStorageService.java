package dev.darkblade.mbe.core.infrastructure.persistence;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import java.util.Collection;

public interface InstanceStorageService {
    void init();
    void close();
    void saveInstance(MultiblockInstance instance);
    void deleteInstance(MultiblockInstance instance);
    Collection<MultiblockInstance> loadAll();
}
