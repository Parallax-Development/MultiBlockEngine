package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.api.addon.AddonContext;
import com.darkbladedev.engine.api.builder.MultiblockBuilder;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.model.condition.Condition;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

public class SimpleAddonContext implements AddonContext {
    private final String addonId;
    private final String addonNamespace;
    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final Logger logger;

    public SimpleAddonContext(String addonId, MultiBlockEngine plugin, MultiblockAPI api, Logger logger) {
        this.addonId = addonId;
        this.addonNamespace = namespaceOf(addonId);
        this.plugin = plugin;
        this.api = api;
        this.logger = logger;
    }

    @Override
    public String getAddonId() {
        return addonId;
    }

    @Override
    public Logger getLogger() {
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
