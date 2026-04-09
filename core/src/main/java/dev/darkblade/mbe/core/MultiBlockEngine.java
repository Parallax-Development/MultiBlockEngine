package dev.darkblade.mbe.core;

import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.core.application.service.api.MultiblockAPIImpl;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.LocaleProvider;
import dev.darkblade.mbe.api.blueprint.BlueprintService;
import dev.darkblade.mbe.api.service.InspectionPipelineService;
import dev.darkblade.mbe.api.service.interaction.InteractionPipelineService;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.io.IOTickService;
import dev.darkblade.mbe.api.tool.mode.ToolModeRegistry;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.api.persistence.PersistentStorageService;
import dev.darkblade.mbe.api.persistence.StorageExceptionHandler;
import dev.darkblade.mbe.api.persistence.StorageRegistry;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.blueprint.BlueprintDefinitionResolver;
import dev.darkblade.mbe.blueprint.BlueprintHeldItemResolver;
import dev.darkblade.mbe.blueprint.BlueprintController;
import dev.darkblade.mbe.blueprint.BlueprintInputListener;
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
import dev.darkblade.mbe.core.infrastructure.integration.MetadataInvalidationListener;
import dev.darkblade.mbe.core.infrastructure.integration.MultiblockExpansion;
import dev.darkblade.mbe.core.infrastructure.integration.PlaceholderCacheInvalidationListener;
import dev.darkblade.mbe.core.infrastructure.integration.IOPortLifecycleListener;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.editor.EditorSessionManager;
import dev.darkblade.mbe.core.application.service.interaction.DefaultInteractionPipelineService;
import dev.darkblade.mbe.core.application.service.assembly.AssemblyReportService;
import dev.darkblade.mbe.core.application.service.assembly.InMemoryAssemblyReportService;
import dev.darkblade.mbe.api.metadata.MetadataAccess;
import dev.darkblade.mbe.api.metadata.MetadataKeyBuilder;
import dev.darkblade.mbe.api.metadata.MetadataService;
import dev.darkblade.mbe.core.application.service.metadata.MetadataServiceImpl;
import dev.darkblade.mbe.core.application.service.metadata.PlayerMultiblockContextResolver;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryService;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryServiceImpl;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitResolver;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitServiceImpl;
import dev.darkblade.mbe.core.application.service.limit.PermissionBasedLimitResolver;
import dev.darkblade.mbe.api.ui.binding.PanelBindingRegistry;
import dev.darkblade.mbe.api.ui.binding.PanelBindingMutationService;
import dev.darkblade.mbe.api.ui.binding.PanelBindingLinkService;
import dev.darkblade.mbe.core.application.service.ui.InteractionRouter;
import dev.darkblade.mbe.core.application.service.ui.PanelBindingService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.infrastructure.config.parser.MultiblockParser;
import dev.darkblade.mbe.core.infrastructure.config.item.ItemConfigLoader;
import dev.darkblade.mbe.core.infrastructure.config.parser.item.ItemConfigParser;
import dev.darkblade.mbe.core.infrastructure.logging.LoggingService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.core.application.service.item.DefaultItemService;
import dev.darkblade.mbe.core.application.service.port.DefaultPortResolutionService;
import dev.darkblade.mbe.core.application.service.io.DefaultIOService;
import dev.darkblade.mbe.core.application.service.io.DefaultIOTickService;
import dev.darkblade.mbe.core.application.service.tool.DefaultToolModeRegistry;
import dev.darkblade.mbe.core.application.service.tool.ToolModeContextResolver;
import dev.darkblade.mbe.core.application.service.tool.ToolModeExecutionService;
import dev.darkblade.mbe.core.application.service.tool.ToolModeMetricsService;
import dev.darkblade.mbe.core.application.service.tool.ToolSessionService;
import dev.darkblade.mbe.core.application.service.tool.WireCutterTool;
import dev.darkblade.mbe.core.application.service.tool.WrenchTool;
import dev.darkblade.mbe.core.application.service.tool.mode.ConfigureChannelMode;
import dev.darkblade.mbe.core.application.service.tool.mode.ConfigureIOMode;
import dev.darkblade.mbe.core.application.service.tool.mode.DebugIOMode;
import dev.darkblade.mbe.core.application.service.tool.mode.DebugWiringMode;
import dev.darkblade.mbe.core.application.service.tool.mode.DisconnectNodesMode;
import dev.darkblade.mbe.core.application.service.tool.mode.LinkPortsMode;
import dev.darkblade.mbe.core.application.service.tool.mode.SplitNetworkMode;
import dev.darkblade.mbe.core.application.service.wrench.DefaultWrenchDispatcher;
import dev.darkblade.mbe.core.application.service.wiring.DefaultNetworkService;
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
import dev.darkblade.mbe.preview.PreviewBlockPlaceListener;
import dev.darkblade.mbe.preview.PreviewSettings;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


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
    private PlayerMultiblockQueryServiceImpl playerMultiblockQueryService;
    private MetadataServiceImpl metadataService;
    private PlayerMultiblockContextResolver metadataContextResolver;

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
        saveResource("items.yml", false);
        saveResource("limits.yml", false);

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
        loadConfiguredItems(itemService, log);
        addonManager.registerCoreService(ItemService.class, itemService);
        ItemStackBridge itemStackBridge = createItemStackBridge(itemService);
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

        PermissionBasedLimitResolver limitResolver = new PermissionBasedLimitResolver(new File(getDataFolder(), "limits.yml"), log);
        MultiblockLimitServiceImpl limitService = new MultiblockLimitServiceImpl(persistence, limitResolver);
        addonManager.registerCoreService(MultiblockLimitResolver.class, limitResolver);
        addonManager.registerCoreService(MultiblockLimitService.class, limitService);
        addonManager.registerCoreMbeService(limitService);

        addonManager.registerCoreService(InspectionPipelineService.class, new DefaultInspectionPipelineService());

        PortResolutionService portResolutionService = new DefaultPortResolutionService();
        addonManager.registerCoreService(PortResolutionService.class, portResolutionService);
        IOService ioService = new DefaultIOService(persistence);
        IOTickService ioTickService = new DefaultIOTickService(ioService);
        NetworkService networkService = new DefaultNetworkService(Bukkit.getPluginManager()::callEvent);
        ToolModeRegistry toolModeRegistry = new DefaultToolModeRegistry();
        ToolSessionService toolSessionService = new ToolSessionService();
        ToolModeMetricsService toolModeMetricsService = new ToolModeMetricsService();
        ToolModeContextResolver toolModeContextResolver = new ToolModeContextResolver(manager, ioService, networkService);
        ToolModeExecutionService toolModeExecutionService = new ToolModeExecutionService(
                itemStackBridge,
                toolModeRegistry,
                toolSessionService,
                toolModeMetricsService,
                Bukkit.getPluginManager()::callEvent
        );
        addonManager.registerCoreService(IOService.class, ioService);
        addonManager.registerCoreService(IOTickService.class, ioTickService);
        addonManager.registerCoreService(NetworkService.class, networkService);
        addonManager.registerCoreService(ToolModeRegistry.class, toolModeRegistry);
        addonManager.registerCoreService(ToolSessionService.class, toolSessionService);
        addonManager.registerCoreService(ToolModeMetricsService.class, toolModeMetricsService);
        addonManager.registerCoreService(ToolModeExecutionService.class, toolModeExecutionService);
        addonManager.registerCoreMbeService(ioService);
        addonManager.registerCoreMbeService(ioTickService);
        addonManager.registerCoreMbeService(toolModeRegistry);
        addonManager.registerCoreMbeService(toolSessionService);
        addonManager.registerCoreMbeService(toolModeMetricsService);
        addonManager.registerCoreMbeService(toolModeExecutionService);

        toolModeRegistry.register(new ConfigureIOMode(ioService, toolModeContextResolver));
        toolModeRegistry.register(new ConfigureChannelMode(ioService, toolModeContextResolver));
        toolModeRegistry.register(new LinkPortsMode(ioService, toolModeContextResolver, toolSessionService));
        toolModeRegistry.register(new DebugIOMode(ioService, toolModeContextResolver));
        toolModeRegistry.register(new DisconnectNodesMode(networkService, toolSessionService, toolModeContextResolver));
        toolModeRegistry.register(new SplitNetworkMode(networkService, toolSessionService, toolModeContextResolver));
        toolModeRegistry.register(new DebugWiringMode(networkService, toolModeContextResolver));

        DefaultAssemblyTriggerRegistry triggerRegistry = new DefaultAssemblyTriggerRegistry();
        BuiltinAssemblyTriggers.registerAll(triggerRegistry);
        int registeredTriggers = triggerRegistry.all().size();
        if (registeredTriggers <= 0) {
            log.error("Assembly triggers registry is empty");
        } else {
            log.info("Assembly triggers registered", dev.darkblade.mbe.api.logging.LogKv.kv("count", registeredTriggers));
        }
        addonManager.registerCoreService(AssemblyTriggerRegistry.class, triggerRegistry);
        assemblyTriggers = triggerRegistry;

        AssemblyReportService assemblyReportService = new InMemoryAssemblyReportService();
        addonManager.registerCoreService(AssemblyReportService.class, assemblyReportService);
        addonManager.registerCoreMbeService(assemblyReportService);
        boolean assemblyDebugEnabled = getConfig().getBoolean("debug.assembly", getConfig().getBoolean("debug", false));
        assemblyCoordinator = new AssemblyCoordinator(manager, triggerRegistry, assemblyReportService, log, assemblyDebugEnabled);
        assemblyCoordinator.setLimitService(limitService);
        if (assemblyCoordinator == null) {
            log.fatal("Assembly coordinator initialization failed");
        }

        WrenchDispatcher wrenchDispatcher = new DefaultWrenchDispatcher(manager, itemStackBridge, i18n, assemblyCoordinator);
        addonManager.registerCoreService(WrenchDispatcher.class, wrenchDispatcher);
        if (wrenchDispatcher instanceof DefaultWrenchDispatcher defaultWrenchDispatcher) {
            defaultWrenchDispatcher.setToolModeExecutionService(toolModeExecutionService);
            defaultWrenchDispatcher.registerToolItem(new WrenchTool());
            defaultWrenchDispatcher.registerToolItem(new WireCutterTool());
        }

        DisplayEntityRenderer displayRenderer = createDisplayRenderer();
        PreviewSettings previewSettings = new PreviewSettings(
            getConfig().getInt("preview.batchSize", 30),
            getConfig().getInt("preview.raycastDistance", 8),
            getConfig().getDouble("preview.maxDistance", 24.0D),
            Duration.ofSeconds(Math.max(3, getConfig().getLong("preview.timeoutSeconds", 20L)))
        );
        structurePreviewService = new StructurePreviewServiceImpl(this, displayRenderer, i18n, new UnknownValidationStrategy(), previewSettings);
        structurePreviewService.start();
        addonManager.registerCoreService(DisplayEntityRenderer.class, displayRenderer);
        registerPlatformPreviewRendererService(displayRenderer);
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
        InteractionPipelineService pipelineService = new DefaultInteractionPipelineService(assemblyCoordinator, wrenchDispatcher, interactionRouter, itemStackBridge);
        addonManager.registerCoreService(InteractionPipelineService.class, pipelineService);
        panelBindings = new PanelBindingService(new File(getDataFolder(), "panel-bindings.yml"), interactionRouter);
        panelBindings.load();
        addonManager.registerCoreService(EditorSessionManager.class, editorSessions);
        addonManager.registerCoreService(PanelBindingService.class, panelBindings);
        addonManager.registerCoreService(PanelBindingRegistry.class, panelBindings);
        addonManager.registerCoreService(PanelBindingMutationService.class, panelBindings);
        addonManager.registerCoreService(PanelBindingLinkService.class, panelBindings);
        long metadataPlaceholderCacheTtlMs = Math.max(0L, getConfig().getLong("metadata.placeholder-cache-ttl-ms", 1000L));
        metadataService = new MetadataServiceImpl(metadataPlaceholderCacheTtlMs);
        addonManager.registerCoreService(MetadataService.class, metadataService);
        metadataContextResolver = new PlayerMultiblockContextResolver(
                manager,
                Math.max(1D, getConfig().getDouble("metadata.placeholder-context-max-distance", 12D))
        );
        registerDefaultMetadata(metadataService);
        long placeholderCacheTtlMs = Math.max(0L, getConfig().getLong("placeholder.cache-ttl-ms", 1000L));
        playerMultiblockQueryService = new PlayerMultiblockQueryServiceImpl(manager, placeholderCacheTtlMs);
        addonManager.registerCoreService(PlayerMultiblockQueryService.class, playerMultiblockQueryService);

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

        // Register Listeners
        InteractionPipelineService interactionPipeline = addonManager.getCoreService(InteractionPipelineService.class);
        getServer().getPluginManager().registerEvents(
                createMultiblockListener(interactionPipeline, i18n),
                this
        );
        getServer().getPluginManager().registerEvents(createEditorInputListener(), this);
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
        getServer().getPluginManager().registerEvents(new PlaceholderCacheInvalidationListener(playerMultiblockQueryService), this);
        getServer().getPluginManager().registerEvents(new MetadataInvalidationListener(metadataService), this);
        getServer().getPluginManager().registerEvents(new IOPortLifecycleListener(ioService, portResolutionService), this);
        for (MultiblockInstance inst : instances) {
            Bukkit.getPluginManager().callEvent(new MultiblockFormEvent(inst, null));
        }

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
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (MultiblockInstance active : manager.getActiveInstancesSnapshot()) {
                ioTickService.tick(active);
            }
        }, 1L, 1L);
        
        // Register PlaceholderExpansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            int placeholderMaxListSize = Math.max(1, getConfig().getInt("placeholder.max-list-size", 50));
            new MultiblockExpansion(this, playerMultiblockQueryService, metadataService, metadataContextResolver, placeholderMaxListSize).register();
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

    private void loadConfiguredItems(DefaultItemService itemService, CoreLogger log) {
        if (itemService == null) {
            return;
        }
        File itemConfigFile = new File(getDataFolder(), "items.yml");
        ItemConfigParser parser = new ItemConfigParser(log);
        ItemConfigLoader loader = new ItemConfigLoader(itemConfigFile, parser, log);
        Map<ItemKey, ItemDefinition> loaded = loader.load();
        int registered = 0;
        for (ItemDefinition definition : loaded.values()) {
            if (definition == null || definition.key() == null) {
                continue;
            }
            itemService.registry().register(definition);
            registered++;
        }
        log.info("Item definitions registered", dev.darkblade.mbe.api.logging.LogKv.kv("count", registered));
    }

    private void registerDefaultMetadata(MetadataService service) {
        service.define(
                MetadataKeyBuilder.<Integer>of("multiblock_energy", Integer.class)
                        .apiAccess(MetadataAccess.READ)
                        .papiAccess(MetadataAccess.READ)
                        .formatter(value -> value + " RF")
                        .visibility(context -> true)
                        .computed(context -> {
                            Object raw = context.instance().getVariable("energy");
                            if (raw instanceof Number number) {
                                return number.intValue();
                            }
                            return 0;
                        })
        );

        service.define(
                MetadataKeyBuilder.<UUID>of("multiblock_owner", UUID.class)
                        .apiAccess(MetadataAccess.READ)
                        .papiAccess(MetadataAccess.READ)
                        .formatter(value -> {
                            OfflinePlayer owner = Bukkit.getOfflinePlayer(value);
                            String name = owner == null ? null : owner.getName();
                            if (name == null || name.isBlank()) {
                                return value.toString();
                            }
                            return name;
                        })
                        .visibility(context -> true)
                        .computed(context -> {
                            Object raw = context.instance().getVariable("owner_uuid");
                            if (raw instanceof UUID uuid) {
                                return uuid;
                            }
                            if (raw instanceof String str && !str.isBlank()) {
                                try {
                                    return UUID.fromString(str.trim());
                                } catch (IllegalArgumentException ignored) {
                                    return new UUID(0L, 0L);
                                }
                            }
                            return new UUID(0L, 0L);
                        })
        );

        service.define(
                MetadataKeyBuilder.<Double>of("multiblock_efficiency", Double.class)
                        .apiAccess(MetadataAccess.READ)
                        .papiAccess(MetadataAccess.READ)
                        .formatter(value -> String.format("%.2f%%", value * 100D))
                        .visibility(context -> true)
                        .computed(context -> {
                            Object energyRaw = context.instance().getVariable("energy");
                            Object maxRaw = context.instance().getVariable("maxEnergy");
                            if (!(energyRaw instanceof Number energyNumber) || !(maxRaw instanceof Number maxNumber)) {
                                return 0D;
                            }
                            double max = maxNumber.doubleValue();
                            if (max <= 0D) {
                                return 0D;
                            }
                            return Math.max(0D, Math.min(1D, energyNumber.doubleValue() / max));
                        })
        );
    }

    private boolean isSqliteDriverPresent() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private DisplayEntityRenderer createDisplayRenderer() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            return new DisplayEntityRenderer() {
                @Override
                public int spawnBlockDisplay(org.bukkit.entity.Player player, org.bukkit.Location location, org.bukkit.block.data.BlockData blockData) {
                    return -1;
                }

                @Override
                public void updateBlockDisplay(int entityId, org.bukkit.block.data.BlockData blockData) {
                }

                @Override
                public void destroyEntities(org.bukkit.entity.Player player, Collection<Integer> entityIds) {
                }
            };
        }
        try {
            Class<?> bridgeClass = Class.forName("dev.darkblade.mbe.platform.bukkit.preview.bridge.ProtocolLibLegacyRendererBridge");
            return (DisplayEntityRenderer) bridgeClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize versioned ProtocolLib renderer bridge", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerPlatformPreviewRendererService(DisplayEntityRenderer displayRenderer) {
        if (displayRenderer == null || addonManager == null) {
            return;
        }
        try {
            Class<?> bridgeType = Class.forName("dev.darkblade.mbe.platform.bukkit.preview.bridge.ProtocolLibLegacyRendererBridge");
            if (!bridgeType.isInstance(displayRenderer)) {
                return;
            }
            Object versionedRenderer = bridgeType.getMethod("delegate").invoke(displayRenderer);
            Class<?> apiType = Class.forName("dev.darkblade.mbe.platform.bukkit.preview.api.BlockDisplayRenderer");
            if (!apiType.isInstance(versionedRenderer)) {
                return;
            }
            addonManager.registerCoreService((Class) apiType, versionedRenderer);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private ItemStackBridge createItemStackBridge(ItemService itemService) {
        try {
            Class<?> bridgeClass = Class.forName("dev.darkblade.mbe.core.infrastructure.bridge.item.PdcItemStackBridge");
            Constructor<?> constructor = bridgeClass.getConstructor(ItemService.class);
            return (ItemStackBridge) constructor.newInstance(itemService);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize Bukkit ItemStack bridge", e);
        }
    }

    private Listener createEditorInputListener() {
        try {
            Class<?> listenerClass = Class.forName("dev.darkblade.mbe.core.platform.listener.EditorInputListener");
            Constructor<?> constructor = listenerClass.getConstructor(MultiBlockEngine.class, EditorSessionManager.class);
            return (Listener) constructor.newInstance(this, editorSessions);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize editor platform listener", e);
        }
    }

    private Listener createMultiblockListener(InteractionPipelineService interactionPipeline, I18nService i18n) {
        try {
            Class<?> listenerClass = Class.forName("dev.darkblade.mbe.core.platform.listener.MultiblockListener");
            Class<?> intentFactoryClass = Class.forName("dev.darkblade.mbe.core.platform.interaction.BukkitInteractionIntentFactory");
            Object intentFactory = intentFactoryClass.getConstructor().newInstance();
            Constructor<?> constructor = listenerClass.getConstructor(
                    MultiblockRuntimeService.class,
                    java.util.function.Consumer.class,
                    AssemblyCoordinator.class,
                    I18nService.class,
                    InteractionPipelineService.class,
                    intentFactoryClass
            );
            java.util.function.Consumer<org.bukkit.event.Event> eventCaller = Bukkit.getPluginManager()::callEvent;
            return (Listener) constructor.newInstance(
                    manager,
                    eventCaller,
                    assemblyCoordinator,
                    i18n,
                    interactionPipeline,
                    intentFactory
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize multiblock platform listener", e);
        }
    }
}
