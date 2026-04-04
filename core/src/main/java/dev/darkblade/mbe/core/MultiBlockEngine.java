package dev.darkblade.mbe.core;

import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.core.application.service.api.MultiblockAPIImpl;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.LocaleProvider;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.blueprint.BlueprintService;
import dev.darkblade.mbe.api.service.InspectionPipelineService;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.api.persistence.PersistentStorageService;
import dev.darkblade.mbe.api.persistence.StorageExceptionHandler;
import dev.darkblade.mbe.api.persistence.StorageRegistry;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.blueprint.BlueprintDefinitionResolver;
import dev.darkblade.mbe.blueprint.BlueprintHeldItemResolver;
import dev.darkblade.mbe.blueprint.BlueprintController;
import dev.darkblade.mbe.blueprint.BlueprintInputListener;
import dev.darkblade.mbe.blueprint.BlueprintItem;
import dev.darkblade.mbe.blueprint.BlueprintServiceImpl;
import dev.darkblade.mbe.blueprint.BuildContextService;
import dev.darkblade.mbe.blueprint.InMemoryBuildContextService;
import dev.darkblade.mbe.blueprint.PreviewPlacementController;
import dev.darkblade.mbe.catalog.CatalogListener;
import dev.darkblade.mbe.catalog.PreviewOriginResolver;
import dev.darkblade.mbe.catalog.RaycastPreviewOriginResolver;
import dev.darkblade.mbe.catalog.StructureCatalogService;
import dev.darkblade.mbe.catalog.StructureCatalogServiceImpl;
import dev.darkblade.mbe.core.application.command.MultiblockCommand;
import dev.darkblade.mbe.core.infrastructure.i18n.BukkitLocaleProvider;
import dev.darkblade.mbe.core.infrastructure.i18n.YamlI18nService;
import dev.darkblade.mbe.core.infrastructure.integration.MultiblockExpansion;
import dev.darkblade.mbe.core.platform.listener.EditorInputListener;
import dev.darkblade.mbe.core.platform.listener.MultiblockListener;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.editor.EditorSessionManager;
import dev.darkblade.mbe.api.ui.binding.PanelBindingRegistry;
import dev.darkblade.mbe.api.ui.binding.PanelBindingMutationService;
import dev.darkblade.mbe.api.ui.binding.PanelBindingLinkService;
import dev.darkblade.mbe.core.application.service.ui.InteractionRouter;
import dev.darkblade.mbe.core.application.service.ui.PanelBindingService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.infrastructure.config.parser.MultiblockParser;
import dev.darkblade.mbe.core.infrastructure.logging.LoggingService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.PdcItemStackBridge;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.core.application.service.item.DefaultItemService;
import dev.darkblade.mbe.core.application.service.port.DefaultPortResolutionService;
import dev.darkblade.mbe.core.application.service.wrench.DefaultWrenchDispatcher;
import dev.darkblade.mbe.core.internal.inspection.DefaultInspectionPipelineService;
import dev.darkblade.mbe.api.command.ExportHookRegistry;
import dev.darkblade.mbe.core.internal.tooling.export.DefaultExportHookRegistry;
import dev.darkblade.mbe.core.internal.tooling.export.ExportConfig;
import dev.darkblade.mbe.core.internal.tooling.export.SelectionService;
import dev.darkblade.mbe.core.internal.tooling.export.StructureExporter;
import dev.darkblade.mbe.core.internal.tooling.export.ExportInteractListener;
import dev.darkblade.mbe.core.infrastructure.persistence.SqlStorage;
import dev.darkblade.mbe.core.infrastructure.persistence.InstanceStorageService;
import dev.darkblade.mbe.core.infrastructure.persistence.FileInstanceStorage;
import dev.darkblade.mbe.core.infrastructure.persistence.DefaultStorageRegistry;
import dev.darkblade.mbe.core.infrastructure.persistence.FilePersistentStorageService;
import dev.darkblade.mbe.preview.DisplayEntityRenderer;
import dev.darkblade.mbe.preview.EntityIdAllocator;
import dev.darkblade.mbe.preview.NoOpDisplayRenderer;
import dev.darkblade.mbe.preview.PreviewBlockPlaceListener;
import dev.darkblade.mbe.preview.PreviewSettings;
import dev.darkblade.mbe.preview.ProtocolLibDisplayRenderer;
import dev.darkblade.mbe.preview.StructurePreviewRequestListener;
import dev.darkblade.mbe.preview.StructurePreviewService;
import dev.darkblade.mbe.preview.StructurePreviewServiceImpl;
import dev.darkblade.mbe.preview.UnknownValidationStrategy;
import dev.darkblade.mbe.uiengine.BlueprintDataProvider;
import dev.darkblade.mbe.uiengine.InventoryConfigLoader;
import dev.darkblade.mbe.uiengine.InventoryDataProvider;
import dev.darkblade.mbe.uiengine.InventoryRenderer;
import dev.darkblade.mbe.uiengine.InventorySessionStore;
import dev.darkblade.mbe.uiengine.InventoryUIListener;
import dev.darkblade.mbe.uiengine.InventoryUIService;
import dev.darkblade.mbe.uiengine.InventoryUIServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import dev.darkblade.mbe.core.internal.debug.DebugSessionService;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import dev.darkblade.mbe.core.domain.assembly.BuiltinAssemblyTriggers;
import dev.darkblade.mbe.core.domain.assembly.DefaultAssemblyTriggerRegistry;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerRegistry;

public class MultiBlockEngine extends JavaPlugin {

    private static final int API_VERSION = 1;

    private static MultiBlockEngine instance;
    private MultiblockRuntimeService manager;
    private MultiblockParser parser;
    private InstanceStorageService storage;
    private PersistentStorageService persistence;
    private MultiblockAPIImpl api;
    private DebugSessionService debugManager;
    private AddonLifecycleService addonManager;
    private LoggingService loggingManager;
    private AssemblyTriggerRegistry assemblyTriggers;
    private AssemblyCoordinator assemblyCoordinator;
    private SelectionService exportSelections;
    private StructureExporter structureExporter;
    private EditorSessionManager editorSessions;
    private PanelBindingService panelBindings;
    private InteractionRouter interactionRouter;
    private StructurePreviewServiceImpl structurePreviewService;
    private StructureCatalogService structureCatalogService;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Save default config
        saveDefaultConfig();
        saveResource("inventories.yml", false);

        loggingManager = new LoggingService(this);
        CoreLogger log = loggingManager.core();
        log.setCorePhase(LogPhase.BOOT);
        log.info("MultiBlockEngine starting...");

        // Initialize components
        api = new MultiblockAPIImpl();
        manager = new MultiblockRuntimeService();
        parser = new MultiblockParser(api, log);
        persistence = new FilePersistentStorageService(getDataFolder().toPath().resolve("persist"));
        persistence.initialize();
        storage = new FileInstanceStorage(this, persistence);
        storage.init();
        persistence.recover();
        manager.setStorage(storage);
        debugManager = new DebugSessionService(this);

        addonManager = new AddonLifecycleService(this, api, log);
        manager.setAddonLifecycleService(addonManager);

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
                new dev.darkblade.mbe.api.logging.LogKv[] {
                    dev.darkblade.mbe.api.logging.LogKv.kv("storage", storageService == null ? "null" : storageService.toString()),
                    dev.darkblade.mbe.api.logging.LogKv.kv("storageType", storageService == null ? "null" : storageService.getClass().getName())
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
        exportSelections = new SelectionService();
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

        DisplayEntityRenderer displayRenderer = getServer().getPluginManager().getPlugin("ProtocolLib") != null
            ? new ProtocolLibDisplayRenderer(new EntityIdAllocator())
            : new NoOpDisplayRenderer();
        PreviewSettings previewSettings = new PreviewSettings(
            getConfig().getInt("preview.batchSize", 30),
            getConfig().getInt("preview.raycastDistance", 8),
            getConfig().getDouble("preview.maxDistance", 24.0D),
            Duration.ofSeconds(Math.max(3, getConfig().getLong("preview.timeoutSeconds", 20L)))
        );
        structurePreviewService = new StructurePreviewServiceImpl(this, displayRenderer, i18n, new UnknownValidationStrategy(), previewSettings);
        structurePreviewService.start();
        addonManager.registerCoreService(StructurePreviewService.class, structurePreviewService);
        structureCatalogService = new StructureCatalogServiceImpl(manager);
        addonManager.registerCoreService(StructureCatalogService.class, structureCatalogService);
        BuildContextService buildContextService = new InMemoryBuildContextService();
        addonManager.registerCoreService(BuildContextService.class, buildContextService);
        BlueprintDefinitionResolver blueprintDefinitionResolver = new BlueprintDefinitionResolver(structureCatalogService);
        Map<String, InventoryDataProvider> uiProviders = new HashMap<>();
        uiProviders.put("blueprints", new BlueprintDataProvider(structureCatalogService));
        InventorySessionStore inventorySessions = new InventorySessionStore();
        InventoryUIService inventoryUIService = new InventoryUIServiceImpl(
                new InventoryConfigLoader(new File(getDataFolder(), "inventories.yml")),
                new InventoryRenderer(uiProviders),
                inventorySessions
        );
        addonManager.registerCoreService(InventoryUIService.class, inventoryUIService);
        PreviewOriginResolver previewOriginResolver = new RaycastPreviewOriginResolver(getConfig().getInt("preview.raycastDistance", 8));
        BlueprintController blueprintController = new BlueprintController(
                buildContextService,
                structurePreviewService,
                blueprintDefinitionResolver,
                previewOriginResolver,
                new BlueprintHeldItemResolver(itemStackBridge)
        );
        BlueprintServiceImpl blueprintService = new BlueprintServiceImpl(blueprintController, inventoryUIService);
        addonManager.registerCoreService(BlueprintService.class, blueprintService);
        addonManager.registerCoreMbeService(blueprintService);
        Bukkit.getServicesManager().register(BlueprintService.class, blueprintService, this, ServicePriority.Normal);
        BlueprintInputListener blueprintInputListener = new BlueprintInputListener(blueprintController);

        editorSessions = new EditorSessionManager();
        interactionRouter = new InteractionRouter();
        panelBindings = new PanelBindingService(new File(getDataFolder(), "panel-bindings.yml"), interactionRouter);
        panelBindings.load();
        addonManager.registerCoreService(EditorSessionManager.class, editorSessions);
        addonManager.registerCoreService(PanelBindingService.class, panelBindings);
        addonManager.registerCoreService(PanelBindingRegistry.class, panelBindings);
        addonManager.registerCoreService(PanelBindingMutationService.class, panelBindings);
        addonManager.registerCoreService(PanelBindingLinkService.class, panelBindings);

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
                log.info("Loaded multiblock", dev.darkblade.mbe.api.logging.LogKv.kv("id", type.id()), dev.darkblade.mbe.api.logging.LogKv.kv("source", loaded.source().type().name()), dev.darkblade.mbe.api.logging.LogKv.kv("path", loaded.source().path()));
            } catch (Exception e) {
                log.log(LogLevel.ERROR, "Failed to register multiblock type", e, dev.darkblade.mbe.api.logging.LogKv.kv("id", type.id()));
            }
        }
        
        // Restore persisted instances
        Collection<MultiblockInstance> instances = storage.loadAll();
        if (instances.isEmpty()) {
            File legacyDb = new File(getDataFolder(), "multiblocks.db");
            if (legacyDb.exists() && legacyDb.isFile() && legacyDb.length() > 0 && isSqliteDriverPresent()) {
                try {
                    InstanceStorageService legacy = new SqlStorage(this);
                    legacy.init();
                    Collection<MultiblockInstance> fromDb = legacy.loadAll();
                    for (MultiblockInstance inst : fromDb) {
                        storage.saveInstance(inst);
                    }
                    legacy.close();
                    instances = fromDb;
                } catch (Throwable t) {
                    log.warn("Legacy SQL migration skipped", dev.darkblade.mbe.api.logging.LogKv.kv("reason", t.getClass().getSimpleName()));
                }
            }
        }
        for (MultiblockInstance inst : instances) {
            manager.registerInstance(inst, false);
        }
        log.info("Restored active instances", dev.darkblade.mbe.api.logging.LogKv.kv("count", instances.size()));

        log.setCorePhase(LogPhase.ENABLE);
        addonManager.enableAddons();

        manager.initializePendingCapabilities();

        for (MultiblockInstance inst : instances) {
            Bukkit.getPluginManager().callEvent(new MultiblockFormEvent(inst, null));
        }
        
        // Register Listeners
        WrenchDispatcher wd = addonManager.getCoreService(WrenchDispatcher.class);
        getServer().getPluginManager().registerEvents(new MultiblockListener(manager, wd, assemblyCoordinator, i18n), this);
        getServer().getPluginManager().registerEvents(new EditorInputListener(this, editorSessions), this);
        getServer().getPluginManager().registerEvents(interactionRouter, this);
        getServer().getPluginManager().registerEvents(new ExportInteractListener(exportSelections), this);
        getServer().getPluginManager().registerEvents(blueprintInputListener, this);
        getServer().getPluginManager().registerEvents(new PreviewPlacementController(blueprintController), this);
        getServer().getPluginManager().registerEvents(new PreviewBlockPlaceListener(structurePreviewService, buildContextService), this);
        getServer().getPluginManager().registerEvents(new StructurePreviewRequestListener(structurePreviewService), this);
        getServer().getPluginManager().registerEvents(new InventoryUIListener(inventorySessions, blueprintService), this);
        getServer().getPluginManager().registerEvents(new CatalogListener(
                blueprintService,
                itemStackBridge
        ), this);

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

        log.info("MultiBlockEngine enabled", dev.darkblade.mbe.api.logging.LogKv.kv("types", types.size()));
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
        if (editorSessions != null) {
            editorSessions.cancelAll();
        }
        if (structurePreviewService != null) {
            structurePreviewService.stop();
        }
        if (panelBindings != null) {
            panelBindings.save();
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
    
    public MultiblockRuntimeService getManager() {
        return manager;
    }
    
    public MultiblockParser getParser() {
        return parser;
    }
    
    public MultiblockAPI getAPI() {
        return api;
    }
    
    public DebugSessionService getDebugSessionService() {
        return debugManager;
    }

    public AddonLifecycleService getAddonLifecycleService() {
        return addonManager;
    }

    public AssemblyTriggerRegistry getAssemblyTriggers() {
        return assemblyTriggers;
    }

    public AssemblyCoordinator getAssemblyCoordinator() {
        return assemblyCoordinator;
    }

    public LoggingService getLoggingService() {
        return loggingManager;
    }

    public SelectionService getExportSelections() {
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
        saveResource("lang/es_es/core.yml", false);
        saveResource("lang/es_es/commands.yml", false);
        saveResource("lang/es_es/services.yml", false);
        saveResource("lang/es_es/items.yml", false);
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
        MessageKey wrenchName = MessageKey.of("mbe", "core.items.wrench.display_name");
        MessageKey wrenchLoreAssemble = MessageKey.of("mbe", "core.items.wrench.lore.assemble");
        MessageKey wrenchLoreDisassemble = MessageKey.of("mbe", "core.items.wrench.lore.disassemble");
        MessageKey wrenchLoreInspect = MessageKey.of("mbe", "core.items.wrench.lore.inspect");
        MessageKey wrenchLoreVariant = MessageKey.of("mbe", "core.items.wrench.lore.variant");
        MessageKey blueprintName = MessageKey.of("mbe", "core.items.blueprint.display_name");
        MessageKey blueprintLoreHold = MessageKey.of("mbe", "core.items.blueprint.lore.hold");
        MessageKey blueprintLoreRightClick = MessageKey.of("mbe", "core.items.blueprint.lore.right_click");
        MessageKey blueprintLoreLeftClick = MessageKey.of("mbe", "core.items.blueprint.lore.left_click");
        ItemDefinition wrench = new ItemDefinition() {
            private final dev.darkblade.mbe.api.item.ItemKey key = ItemKeys.of("mbe:wrench", 0);

            @Override
            public dev.darkblade.mbe.api.item.ItemKey key() {
                return key;
            }

            @Override
            public String displayName() {
                return wrenchName.fullKey();
            }

            @Override
            public Map<String, Object> properties() {
                return Map.of(
                        "material", "IRON_HOE",
                        "unstackable", false,
                        "lore", List.of(
                                wrenchLoreAssemble.fullKey(),
                                wrenchLoreDisassemble.fullKey(),
                                wrenchLoreInspect.fullKey(),
                                wrenchLoreVariant.fullKey()
                        )
                );
            }
        };

        itemService.registry().register(wrench);
        ItemDefinition blueprint = new ItemDefinition() {
            private final dev.darkblade.mbe.api.item.ItemKey key = BlueprintItem.BLUEPRINT_KEY;

            @Override
            public dev.darkblade.mbe.api.item.ItemKey key() {
                return key;
            }

            @Override
            public String displayName() {
                return blueprintName.fullKey();
            }

            @Override
            public Map<String, Object> properties() {
                return Map.of(
                    "material", "PAPER",
                    "unstackable", true,
                    "lore", List.of(
                        blueprintLoreHold.fullKey(),
                        blueprintLoreRightClick.fullKey(),
                        blueprintLoreLeftClick.fullKey()
                    )
                );
            }
        };
        itemService.registry().register(blueprint);
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
