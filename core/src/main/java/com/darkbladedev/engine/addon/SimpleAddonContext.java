package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.api.addon.AddonContext;
import com.darkbladedev.engine.api.builder.MultiblockBuilder;
import com.darkbladedev.engine.api.logging.AddonLogger;
import com.darkbladedev.engine.api.wrench.WrenchDispatcher;
import com.darkbladedev.engine.api.wrench.WrenchInteractable;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.model.condition.Condition;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class SimpleAddonContext implements AddonContext {
    private final String addonId;
    private final String addonNamespace;
    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final AddonLogger logger;
    private final Path dataFolder;
    private final AddonManager addonManager;
    private final AddonServiceRegistry services;

    public SimpleAddonContext(String addonId, MultiBlockEngine plugin, MultiblockAPI api, AddonLogger logger, Path dataFolder, AddonManager addonManager, AddonServiceRegistry services) {
        this.addonId = addonId;
        this.addonNamespace = namespaceOf(addonId);
        this.plugin = plugin;
        this.api = api;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.addonManager = addonManager;
        this.services = services;
    }

    @Override
    public String getAddonId() {
        return addonId;
    }

    @Override
    public AddonLogger getLogger() {
        return logger;
    }

    @Override
    public String getEngineVersion() {
        return MultiBlockEngine.getInstance().getPluginMeta().getVersion();
    }

    @Override
    public int getApiVersion() {
        return MultiBlockEngine.getApiVersion();
    }

    @Override
    public MultiblockAPI getAPI() {
        return api;
    }

    @Override
    public Path getDataFolder() {
        return dataFolder;
    }

    @Override
    public <T> void registerService(Class<T> serviceType, T service) {
        services.register(addonId, serviceType, service);
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return services.resolveIfEnabled(addonId, serviceType, addonManager::getState).orElse(null);
    }

    @Override
    public <T> void exposeService(Class<T> api, T implementation, ServicePriority priority) {
        addonManager.queueServiceExposure(addonId, api, implementation, priority);
    }

    @Override
    public void registerAction(String key, Function<Map<String, Object>, Action> factory) {
        if (!key.startsWith(addonNamespace + ":")) {
            throw new IllegalArgumentException("Action key must start with addon namespace prefix: " + addonNamespace + ":");
        }
        api.registerAction(key, config -> Action.owned(addonId, key, factory.apply(config)));
    }

    @Override
    public void registerCondition(String key, Function<Map<String, Object>, Condition> factory) {
        if (!key.startsWith(addonNamespace + ":")) {
            throw new IllegalArgumentException("Condition key must start with addon namespace prefix: " + addonNamespace + ":");
        }
        api.registerCondition(key, config -> Condition.owned(addonId, key, factory.apply(config)));
    }

    @Override
    public void registerWrenchAction(String key, WrenchInteractable interactable) {
        if (!key.startsWith(addonNamespace + ":")) {
            throw new IllegalArgumentException("Wrench action key must start with addon namespace prefix: " + addonNamespace + ":");
        }

        WrenchDispatcher dispatcher = getService(WrenchDispatcher.class);
        if (dispatcher == null) {
            throw new IllegalStateException("WrenchDispatcher service is not available");
        }

        dispatcher.registerAction(key, interactable);
    }

    @Override
    public void registerMatcher(String prefix, Function<String, BlockMatcher> factory) {
        if (!addonNamespace.equalsIgnoreCase(prefix)) {
            throw new IllegalArgumentException("Matcher prefix must equal addon namespace: " + addonNamespace);
        }
        api.registerMatcher(prefix, factory);
    }

    @Override
    public void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public MultiblockBuilder createMultiblock(String id) {
        String fullId = id.contains(":") ? id : addonNamespace + ":" + id;
        if (!fullId.startsWith(addonNamespace + ":")) {
            throw new IllegalArgumentException("Multiblock id must start with addon namespace prefix: " + addonNamespace + ":");
        }
        return new MultiblockBuilder(fullId, addonId);
    }

    @Override
    public void registerMultiblock(MultiblockType type) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (!type.id().startsWith(addonNamespace + ":")) {
            throw new IllegalArgumentException("MultiblockType id must start with addon namespace prefix: " + addonNamespace + ":");
        }
        api.registerMultiblock(type);
    }

    private static String namespaceOf(String addonId) {
        if (addonId == null) {
            throw new IllegalArgumentException("addonId");
        }
        int idx = addonId.indexOf(':');
        return idx < 0 ? addonId : addonId.substring(0, idx);
    }

    @Override
    public void runTask(Runnable task) {
        MultiBlockEngine.getInstance().getServer().getScheduler().runTask(MultiBlockEngine.getInstance(), task);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        MultiBlockEngine.getInstance().getServer().getScheduler().runTaskAsynchronously(MultiBlockEngine.getInstance(), task);
    }
}
