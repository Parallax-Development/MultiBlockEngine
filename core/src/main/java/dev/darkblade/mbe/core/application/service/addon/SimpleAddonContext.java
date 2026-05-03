package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.api.addon.AddonContext;
import dev.darkblade.mbe.api.assembly.MultiblockBuilder;
import dev.darkblade.mbe.api.compat.SchedulerCompatService;
import dev.darkblade.mbe.api.logging.AddonLogger;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.ServiceListener;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.api.command.WrenchInteractable;
import dev.darkblade.mbe.core.application.service.ServiceLifecycleOrchestrator;
import dev.darkblade.mbe.core.domain.BlockMatcher;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.condition.Condition;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SimpleAddonContext implements AddonContext {
    private final String addonId;
    private final String addonNamespace;
    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final AddonLogger logger;
    private final Path dataFolder;
    private final AddonLifecycleService addonManager;
    private final AddonServiceRegistry services;
    private final ServiceLifecycleOrchestrator serviceLifecycleManager;

    public SimpleAddonContext(
            String addonId,
            MultiBlockEngine plugin,
            MultiblockAPI api,
            AddonLogger logger,
            Path dataFolder,
            AddonLifecycleService addonManager,
            AddonServiceRegistry services,
            ServiceLifecycleOrchestrator serviceLifecycleManager
    ) {
        this.addonId = addonId;
        this.addonNamespace = namespaceOf(addonId);
        this.plugin = plugin;
        this.api = api;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.addonManager = addonManager;
        this.services = services;
        this.serviceLifecycleManager = serviceLifecycleManager;
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
        addonManager.registerAddonTypedService(addonId, serviceType, service);
    }

    @Override // Required Services
    public <T> T getService(Class<T> serviceType) {
        List<T> dynamic = serviceLifecycleManager.getByType(serviceType);
        if (!dynamic.isEmpty()) {
            return dynamic.get(0);
        }
        return services.resolveIfEnabled(addonId, serviceType, addonManager::getState).orElse(null);
    }

    @Override
    public void registerService(MBEService service) {
        serviceLifecycleManager.registerService(addonId, service);
        addonManager.registerAddonMbeService(addonId, service);
    }

    @Override // Optional Services
    public <T> Optional<T> getService(String serviceId, Class<T> serviceType) {
        return serviceLifecycleManager.get(serviceId, serviceType);
    }

    @Override
    public <T> List<T> getServicesByType(Class<T> serviceType) {
        return serviceLifecycleManager.getByType(serviceType);
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        serviceLifecycleManager.addListener(listener);
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        serviceLifecycleManager.removeListener(listener);
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
        SchedulerCompatService scheduler = addonManager.getCoreService(SchedulerCompatService.class);
        if (scheduler != null) {
            scheduler.runSync(task);
            return;
        }
        MultiBlockEngine.getInstance().getServer().getScheduler().runTask(MultiBlockEngine.getInstance(), task);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        SchedulerCompatService scheduler = addonManager.getCoreService(SchedulerCompatService.class);
        if (scheduler != null) {
            scheduler.runAsync(task);
            return;
        }
        MultiBlockEngine.getInstance().getServer().getScheduler().runTaskAsynchronously(MultiBlockEngine.getInstance(), task);
    }

    @Override
    public void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter tabCompleter, String... aliases) {
        try {
            final java.lang.reflect.Field f = plugin.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) f.get(plugin.getServer());
            org.bukkit.command.Command cmd = new org.bukkit.command.Command(name) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                    return executor.onCommand(sender, this, commandLabel, args);
                }

                @Override
                public List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                    if (tabCompleter != null) {
                        List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
                        if (completions != null) {
                            return completions;
                        }
                    }
                    return super.tabComplete(sender, alias, args);
                }
            };
            if (aliases != null && aliases.length > 0) {
                cmd.setAliases(java.util.Arrays.asList(aliases));
            }
            commandMap.register(addonNamespace, cmd);
        } catch (Exception e) {
            logger.error("Failed to register command " + name + " for addon " + addonId, e);
        }
    }
}
