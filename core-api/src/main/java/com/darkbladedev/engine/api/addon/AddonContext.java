package com.darkbladedev.engine.api.addon;

import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.api.builder.MultiblockBuilder;
import com.darkbladedev.engine.api.logging.AddonLogger;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.model.condition.Condition;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public interface AddonContext {
    String getAddonId();
    AddonLogger getLogger();
    String getEngineVersion();
    int getApiVersion();
    MultiblockAPI getAPI();

    Path getDataFolder();

    <T> void registerService(Class<T> serviceType, T service);
    <T> T getService(Class<T> serviceType);

    <T> void exposeService(Class<T> api, T implementation, ServicePriority priority);

    default <T> void exposeService(Class<T> api, T implementation) {
        exposeService(api, implementation, ServicePriority.Normal);
    }
    
    void registerAction(String key, Function<Map<String, Object>, Action> factory);
    void registerCondition(String key, Function<Map<String, Object>, Condition> factory);
    void registerMatcher(String prefix, Function<String, BlockMatcher> factory);
    void registerListener(Listener listener);
    MultiblockBuilder createMultiblock(String id);
    void registerMultiblock(MultiblockType type);
    
    void runTask(Runnable task);
    void runTaskAsync(Runnable task);
}
