package com.darkbladedev.engine;

import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.api.impl.MultiblockAPIImpl;
import com.darkbladedev.engine.addon.AddonManager;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.api.item.ItemService;
import com.darkbladedev.engine.api.item.ItemDefinition;
import com.darkbladedev.engine.api.item.ItemKeys;
import com.darkbladedev.engine.api.i18n.I18nService;
import com.darkbladedev.engine.api.i18n.LocaleProvider;
import com.darkbladedev.engine.api.inspection.InspectionPipelineService;
import com.darkbladedev.engine.api.port.PortResolutionService;
import com.darkbladedev.engine.api.wrench.WrenchDispatcher;
import com.darkbladedev.engine.api.persistence.PersistentStorageService;
import com.darkbladedev.engine.api.storage.StorageExceptionHandler;
import com.darkbladedev.engine.api.storage.StorageRegistry;
import com.darkbladedev.engine.api.event.MultiblockFormEvent;
import com.darkbladedev.engine.command.MultiblockCommand;
import com.darkbladedev.engine.i18n.BukkitLocaleProvider;
import com.darkbladedev.engine.i18n.YamlI18nService;
import com.darkbladedev.engine.integration.MultiblockExpansion;
import com.darkbladedev.engine.listener.MultiblockListener;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.parser.MultiblockParser;
import com.darkbladedev.engine.logging.LoggingManager;
import com.darkbladedev.engine.item.bridge.PdcItemStackBridge;
import com.darkbladedev.engine.item.bridge.ItemStackBridge;
import com.darkbladedev.engine.item.DefaultItemService;
import com.darkbladedev.engine.port.DefaultPortResolutionService;
import com.darkbladedev.engine.wrench.DefaultWrenchDispatcher;
import com.darkbladedev.engine.inspection.DefaultInspectionPipelineService;
import com.darkbladedev.engine.api.export.ExportHookRegistry;
import com.darkbladedev.engine.export.DefaultExportHookRegistry;
import com.darkbladedev.engine.export.ExportConfig;
import com.darkbladedev.engine.export.SelectionManager;
import com.darkbladedev.engine.export.StructureExporter;
import com.darkbladedev.engine.export.ExportInteractListener;
import com.darkbladedev.engine.storage.SqlStorage;
import com.darkbladedev.engine.storage.StorageManager;
import com.darkbladedev.engine.storage.FileInstanceStorage;
import com.darkbladedev.engine.storage.service.DefaultStorageRegistry;
import com.darkbladedev.engine.persistence.FilePersistentStorageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import com.darkbladedev.engine.debug.DebugManager;
import com.darkbladedev.engine.assembly.AssemblyCoordinator;
import com.darkbladedev.engine.assembly.BuiltinAssemblyTriggers;
import com.darkbladedev.engine.assembly.DefaultAssemblyTriggerRegistry;
import com.darkbladedev.engine.api.assembly.AssemblyTriggerRegistry;

public class MultiBlockEngine extends JavaPlugin {

    private static final int API_VERSION = 1;

    private static MultiBlockEngine instance;
    private MultiblockManager manager;
    private MultiblockParser parser;
    private StorageManager storage;
    private PersistentStorageService persistence;
    private MultiblockAPIImpl api;
    private DebugManager debugManager;
    private AddonManager addonManager;
    private LoggingManager loggingManager;
    private AssemblyTriggerRegistry assemblyTriggers;
    private AssemblyCoordinator assemblyCoordinator;
    private SelectionManager exportSelections;
    private StructureExporter structureExporter;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Save default config
        saveDefaultConfig();

        loggingManager = new LoggingManager(this);
        CoreLogger log = loggingManager.core();
        log.setCorePhase(LogPhase.BOOT);
        log.info("MultiBlockEngine starting...");

        // Initialize components
        api = new MultiblockAPIImpl();
        manager = new MultiblockManager();
        parser = new MultiblockParser(api, log);
        persistence = new FilePersistentStorageService(getDataFolder().toPath().resolve("persist"));
        persistence.initialize();
        storage = new FileInstanceStorage(this, persistence);
        storage.init();
        persistence.recover();
        manager.setStorage(storage);
        debugManager = new DebugManager(this);

        addonManager = new AddonManager(this, api, log);
        manager.setAddonManager(addonManager);

        StorageExceptionHandler storageExceptionHandler = (storageService, error) -> {
            if (error == null) {
                return;
            }

            log.logInternal(
                new LogScope.Core(),
                LogPhase.RUNTIME,
                LogLevel.ERROR,
                "Storage service exception",
                error,
                new com.darkbladedev.engine.api.logging.LogKv[] {
                    com.darkbladedev.engine.api.logging.LogKv.kv("storage", storageService == null ? "null" : storageService.toString()),
                    com.darkbladedev.engine.api.logging.LogKv.kv("storageType", storageService == null ? "null" : storageService.getClass().getName())
                },
                Set.of("storage")
            );
        };

        DefaultItemService itemService = new DefaultItemService();
        registerCoreItems(itemService);
        addonManager.registerCoreService(ItemService.class, itemService);
        ItemStackBridge itemStackBridge = new PdcItemStackBridge(itemService);
        addonManager.registerCoreService(ItemStackBridge.class, itemStackBridge);
        addonManager.registerCoreService(StorageRegistry.class, new DefaultStorageRegistry(log, storageExceptionHandler));
        addonManager.registerCoreService(PersistentStorageService.class, persistence);

        ExportHookRegistry exportHooks = new DefaultExportHookRegistry();
        addonManager.registerCoreService(ExportHookRegistry.class, exportHooks);

        ExportConfig exportConfig = ExportConfig.from(getConfig().getConfigurationSection("export"));
        exportSelections = new SelectionManager();
        structureExporter = new StructureExporter(log, exportHooks, exportConfig);

        LocaleProvider localeProvider = new BukkitLocaleProvider(Locale.forLanguageTag("en-US"));
        I18nService i18n = new YamlI18nService(
                getDataFolder(),
                () -> addonManager.listLoadedAddons().stream()
                        .map(a -> new YamlI18nService.I18nSource(a.id(), a.dataFolder().toFile()))
                        .toList(),
                log,
                localeProvider,
                () -> getConfig().getBoolean("i18n.debugMissingKeys", false)
        );
        addonManager.registerCoreService(LocaleProvider.class, localeProvider);
        addonManager.registerCoreService(I18nService.class, i18n);

        addonManager.registerCoreService(InspectionPipelineService.class, new DefaultInspectionPipelineService());

        addonManager.registerCoreService(PortResolutionService.class, new DefaultPortResolutionService());

        DefaultAssemblyTriggerRegistry triggerRegistry = new DefaultAssemblyTriggerRegistry();
        BuiltinAssemblyTriggers.registerAll(triggerRegistry);
        addonManager.registerCoreService(AssemblyTriggerRegistry.class, triggerRegistry);
        assemblyTriggers = triggerRegistry;

        assemblyCoordinator = new AssemblyCoordinator(manager, triggerRegistry, log);

        WrenchDispatcher wrenchDispatcher = new DefaultWrenchDispatcher(manager, itemStackBridge, i18n, assemblyCoordinator);
        addonManager.registerCoreService(WrenchDispatcher.class, wrenchDispatcher);

        addonManager.loadAddons();

        ensureDefaultLangFiles();
        ensureDefaultMultiblockFiles();
        i18n.reload();
        
        // Ensure directory exists
        File multiblockDir = new File(getDataFolder(), "multiblocks");
        if (!multiblockDir.exists()) {
            multiblockDir.mkdirs();
        }
        
        // Load definitions
        log.setCorePhase(LogPhase.LOAD);
        List<MultiblockParser.LoadedType> loadedTypes = parser.loadAllWithSources(multiblockDir);
        List<MultiblockType> types = new ArrayList<>(loadedTypes.size());
        for (MultiblockParser.LoadedType loaded : loadedTypes) {
            if (loaded == null || loaded.type() == null) {
                continue;
            }
            MultiblockType type = loaded.type();
            try {
                manager.registerType(type, loaded.source());
                types.add(type);
                log.info("Loaded multiblock", com.darkbladedev.engine.api.logging.LogKv.kv("id", type.id()), com.darkbladedev.engine.api.logging.LogKv.kv("source", loaded.source().type().name()), com.darkbladedev.engine.api.logging.LogKv.kv("path", loaded.source().path()));
            } catch (Exception e) {
                log.log(LogLevel.ERROR, "Failed to register multiblock type", e, com.darkbladedev.engine.api.logging.LogKv.kv("id", type.id()));
            }
        }
        
        // Restore persisted instances
        Collection<MultiblockInstance> instances = storage.loadAll();
        if (instances.isEmpty()) {
            File legacyDb = new File(getDataFolder(), "multiblocks.db");
            if (legacyDb.exists() && legacyDb.isFile() && legacyDb.length() > 0 && isSqliteDriverPresent()) {
                try {
                    StorageManager legacy = new SqlStorage(this);
                    legacy.init();
                    Collection<MultiblockInstance> fromDb = legacy.loadAll();
                    for (MultiblockInstance inst : fromDb) {
                        storage.saveInstance(inst);
                    }
                    legacy.close();
                    instances = fromDb;
                } catch (Throwable t) {
                    log.warn("Legacy SQL migration skipped", com.darkbladedev.engine.api.logging.LogKv.kv("reason", t.getClass().getSimpleName()));
                }
            }
        }
        for (MultiblockInstance inst : instances) {
            manager.registerInstance(inst, false);
        }
        log.info("Restored active instances", com.darkbladedev.engine.api.logging.LogKv.kv("count", instances.size()));

        log.setCorePhase(LogPhase.ENABLE);
        addonManager.enableAddons();

        manager.initializePendingCapabilities();

        for (MultiblockInstance inst : instances) {
            Bukkit.getPluginManager().callEvent(new MultiblockFormEvent(inst, null));
        }
        
        // Register Listeners
        WrenchDispatcher wd = addonManager.getCoreService(WrenchDispatcher.class);
        getServer().getPluginManager().registerEvents(new MultiblockListener(manager, wd, assemblyCoordinator), this);
        
        getServer().getPluginManager().registerEvents(new ExportInteractListener(exportSelections), this);

        // Register Commands
        MultiblockCommand cmd = new MultiblockCommand(this, exportSelections, structureExporter);
        getCommand("multiblock").setExecutor(cmd);
        getCommand("multiblock").setTabCompleter(cmd);
        
        // Start Ticking
        manager.startTicking(this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (persistence != null) {
                    persistence.flush();
                }
            } catch (Exception ignored) {
            }
        }, 20L * 60L, 20L * 60L);
        
        // Register PlaceholderExpansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MultiblockExpansion(this).register();
            log.info("Hooked into PlaceholderAPI");
        }

        log.info("MultiBlockEngine enabled", com.darkbladedev.engine.api.logging.LogKv.kv("types", types.size()));
    }

    @Override
    public void onDisable() {
        CoreLogger log = loggingManager != null ? loggingManager.core() : null;
        if (log != null) {
            log.setCorePhase(LogPhase.DISABLE);
        }

        Bukkit.getScheduler().cancelTasks(this);
        if (debugManager != null) {
            debugManager.stopAll();
        }
        if (addonManager != null) {
            addonManager.disableAddons();
        }
        if (manager != null) {
            manager.unregisterAll();
        }
        if (storage != null) {
            storage.close();
        }
        if (persistence != null) {
            persistence.shutdown(true);
        }
        if (log != null) {
            log.info("MultiBlockEngine stopping...");
        } else {
            getLogger().info("MultiBlockEngine stopping...");
        }
    }
    
    public static MultiBlockEngine getInstance() {
        return instance;
    }

    public static int getApiVersion() {
        return API_VERSION;
    }
    
    public MultiblockManager getManager() {
        return manager;
    }
    
    public MultiblockParser getParser() {
        return parser;
    }
    
    public MultiblockAPI getAPI() {
        return api;
    }
    
    public DebugManager getDebugManager() {
        return debugManager;
    }

    public AddonManager getAddonManager() {
        return addonManager;
    }

    public AssemblyTriggerRegistry getAssemblyTriggers() {
        return assemblyTriggers;
    }

    public AssemblyCoordinator getAssemblyCoordinator() {
        return assemblyCoordinator;
    }

    public LoggingManager getLoggingManager() {
        return loggingManager;
    }

    public SelectionManager getExportSelections() {
        return exportSelections;
    }

    public StructureExporter getStructureExporter() {
        return structureExporter;
    }

    private void ensureDefaultLangFiles() {
        saveResource("lang/en_us/core.yml", false);
        saveResource("lang/en_us/commands.yml", false);
        saveResource("lang/en_us/services.yml", false);
        saveResource("lang/en_us/items.yml", false);
        saveResource("lang/en_us/ui.yml", false);
        saveResource("lang/es_es/core.yml", false);
        saveResource("lang/es_es/commands.yml", false);
        saveResource("lang/es_es/services.yml", false);
        saveResource("lang/es_es/items.yml", false);
        saveResource("lang/es_es/ui.yml", false);
    }

    private void ensureDefaultMultiblockFiles() {
        File defaultDir = getDataFolder().toPath().resolve("multiblocks").resolve(".default").toFile();
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        saveResource("multiblocks/.default/base_machine.yml", false);
        saveResource("multiblocks/.default/mana_generator.yml", false);
        saveResource("multiblocks/.default/example_portal.yml", false);
        saveResource("multiblocks/.default/healer_machine.yml", false);
        saveResource("multiblocks/.default/miner_machine.yml", false);
        saveResource("multiblocks/.default/test_action.yml", false);
        saveResource("multiblocks/.default/test_complex.yml", false);
        saveResource("multiblocks/.default/test_optional.yml", false);
        saveResource("multiblocks/.default/test_ticking.yml", false);
    }

    private static void registerCoreItems(DefaultItemService itemService) {
        if (itemService == null) {
            return;
        }
        ItemDefinition wrench = new ItemDefinition() {
            private final com.darkbladedev.engine.api.item.ItemKey key = ItemKeys.of("mbe:wrench", 0);

            @Override
            public com.darkbladedev.engine.api.item.ItemKey key() {
                return key;
            }

            @Override
            public String displayName() {
                return "Wrench";
            }

            @Override
            public Map<String, Object> properties() {
                return Map.of(
                        "material", "IRON_HOE",
                        "unstackable", false,
                        "lore", List.of(
                                "&eClick derecho: &aEnsamblar multibloque",
                                "&eClick izquierdo: &cDesensamblar multibloque",
                                "&eShift + Click derecho: &bMostrar información",
                                "&eShift + Click izquierdo: &dCambiar variante"
                        )
                );
            }
        };

        itemService.registry().register(wrench);
    }

    private boolean isSqliteDriverPresent() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
