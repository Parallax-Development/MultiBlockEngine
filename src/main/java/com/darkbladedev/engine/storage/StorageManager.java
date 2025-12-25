package com.darkbladedev.engine.storage;

import com.darkbladedev.engine.model.MultiblockInstance;
import java.util.Collection;

public interface StorageManager {
    void init();
    void close();
    void saveInstance(MultiblockInstance instance);
    void deleteInstance(MultiblockInstance instance);
    Collection<MultiblockInstance> loadAll();
}
