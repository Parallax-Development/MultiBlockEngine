package dev.darkblade.mbe.core.application.command.misc;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.command.MBECommandManager;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.MetricsService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import dev.darkblade.mbe.core.application.command.service.impl.AssemblyCommandService;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.assembly.AssemblyStepTrace;
import dev.darkblade.mbe.core.infrastructure.config.parser.MultiblockParser;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.api.service.InteractionSource;
import dev.darkblade.mbe.api.service.InspectionPipelineService;
import dev.darkblade.mbe.api.service.Inspectable;
import dev.darkblade.mbe.api.service.InspectionRenderer;
import dev.darkblade.mbe.api.service.InspectionData;
import dev.darkblade.mbe.api.service.InspectionEntry;
import dev.darkblade.mbe.api.service.InspectionLevel;
import dev.darkblade.mbe.api.service.EntryType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MiscCommand {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_STATUS_TITLE = MessageKey.of(ORIGIN, "commands.status.title");
    private static final MessageKey MSG_STATUS_LOADED_TYPES_VALUE = MessageKey.of(ORIGIN, "commands.status.loaded_types");
    private static final MessageKey MSG_STATUS_TOTAL_CREATED_VALUE = MessageKey.of(ORIGIN, "commands.status.created_instances");
    private static final MessageKey MSG_STATUS_TOTAL_DESTROYED_VALUE = MessageKey.of(ORIGIN, "commands.status.destroyed_instances");
    private static final MessageKey MSG_STATUS_STRUCTURE_CHECKS_VALUE = MessageKey.of(ORIGIN, "commands.status.structure_checks");
    private static final MessageKey MSG_STATUS_AVG_TICK_VALUE = MessageKey.of(ORIGIN, "commands.status.avg_tick_time");

    private static final MessageKey MSG_RELOAD_START = MessageKey.of(ORIGIN, "commands.reload.start");
    private static final MessageKey MSG_RELOAD_DONE_TYPES = MessageKey.of(ORIGIN, "commands.reload.types");
    private static final MessageKey MSG_RELOAD_DONE_RESTART = MessageKey.of(ORIGIN, "commands.reload.restart");

    private static final MessageKey MSG_REPORT_PLAYER_NOT_FOUND = MessageKey.of(ORIGIN, "commands.report.player_not_found");
    private static final MessageKey MSG_REPORT_NONE = MessageKey.of(ORIGIN, "commands.report.none");
    private static final MessageKey MSG_REPORT_TITLE = MessageKey.of(ORIGIN, "commands.report.title");
    private static final MessageKey MSG_REPORT_RESULT = MessageKey.of(ORIGIN, "commands.report.result");
    private static final MessageKey MSG_REPORT_REASON = MessageKey.of(ORIGIN, "commands.report.reason");
    private static final MessageKey MSG_REPORT_TRIGGER = MessageKey.of(ORIGIN, "commands.report.trigger");
    private static final MessageKey MSG_REPORT_MULTIBLOCK = MessageKey.of(ORIGIN, "commands.report.multiblock");
    private static final MessageKey MSG_REPORT_DEBUG_TITLE = MessageKey.of(ORIGIN, "commands.report.debug.title");
    private static final MessageKey MSG_REPORT_DEBUG_LINE = MessageKey.of(ORIGIN, "commands.report.debug.line");
    private static final MessageKey MSG_REPORT_TRACE_TITLE = MessageKey.of(ORIGIN, "commands.report.trace.title");
    private static final MessageKey MSG_REPORT_TRACE_LINE = MessageKey.of(ORIGIN, "commands.report.trace.line");

    private final MultiBlockEngine plugin;
    private final MBECommandManager manager;
    private final PlayerMessageService messageService;

    public MiscCommand(MultiBlockEngine plugin, MBECommandManager manager, PlayerMessageService messageService) {
        this.plugin = plugin;
        this.manager = manager;
        this.messageService = messageService;
    }

    public void register() {
        Command.Builder<dev.darkblade.mbe.core.application.command.MBESender> builder = manager.commandBuilder("mbe", "multiblock");

        // Status
        manager.command(builder.literal("status")
                .permission("multiblockengine.status")
                .handler(context -> handleStatus(context.sender().getSender()))
        );

        // Stats alias
        manager.command(builder.literal("stats")
                .permission("multiblockengine.status")
                .handler(context -> handleStatus(context.sender().getSender()))
        );

        // Reload
        manager.command(builder.literal("reload")
                .permission("multiblockengine.admin.reload")
                .handler(context -> handleReload(context.sender().getSender()))
        );

        // Assemble
        AssemblyCommandService assemblyCommands = new AssemblyCommandService(plugin);
        manager.command(builder.literal("assemble")
                .permission("multiblockengine.assemble")
                .handler(context -> {
                    if (context.sender().isPlayer()) assemblyCommands.executeAssemble(context.sender().getPlayer());
                })
        );

        // Disassemble
        manager.command(builder.literal("disassemble")
                .permission("multiblockengine.disassemble")
                .handler(context -> {
                    if (context.sender().isPlayer()) assemblyCommands.executeDisassemble(context.sender().getPlayer());
                })
        );

        // Report
        manager.command(builder.literal("report")
                .permission("multiblockengine.report")
                .optional("target", StringParser.stringParser())
                .flag(manager.flagBuilder("console").withAliases("c").build())
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    String targetName = context.getOrDefault("target", null);
                    boolean console = context.flags().isPresent("console");
                    handleReport(sender, targetName, console);
                })
        );

        // UI
        manager.command(builder.literal("ui")
                .literal("debug")
                .literal("panels")
                .permission("multiblockengine.ui.debug")
                .handler(context -> handleUi(context.sender().getSender()))
        );

        // Addons wrapper
        dev.darkblade.mbe.core.application.command.addon.AddonsCommandRouter addons = new dev.darkblade.mbe.core.application.command.addon.AddonsCommandRouter(plugin);
        manager.command(builder.literal("addons")
                .permission("multiblockengine.admin.addons")
                .optional("args", StringParser.greedyStringParser())
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    String argsStr = context.getOrDefault("args", "");
                    String[] rawArgs = argsStr.isEmpty() ? new String[]{"addons"} : ("addons " + argsStr).split(" ");
                    addons.handle(sender, "mbe", rawArgs);
                })
        );

        // Services wrapper
        dev.darkblade.mbe.core.application.command.service.ServicesCommandRouter services = new dev.darkblade.mbe.core.application.command.service.ServicesCommandRouter(plugin, plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.event.EventBusService.class));
        services.registerInternal(new dev.darkblade.mbe.core.application.command.service.impl.UiCommandService(plugin));
        services.registerInternal(new dev.darkblade.mbe.core.application.command.service.impl.ItemsCommandService(plugin));
        services.registerInternal(new dev.darkblade.mbe.core.application.command.service.impl.BlueprintCommandService(plugin));
        services.registerInternal(new dev.darkblade.mbe.core.application.command.service.impl.AssemblyCommandService(plugin));

        manager.command(builder.literal("services")
                .permission("multiblockengine.admin.services")
                .optional("args", StringParser.greedyStringParser())
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    String argsStr = context.getOrDefault("args", "");
                    String[] rawArgs = argsStr.isEmpty() ? new String[]{"services"} : ("services " + argsStr).split(" ");
                    services.handle(sender, "mbe", rawArgs);
                })
        );

        manager.command(builder.literal("inspect")
                .permission("multiblockengine.inspect")
                .optional("level", StringParser.stringParser())
                .handler(context -> {
                    if (!context.sender().isPlayer()) return;
                    Player player = context.sender().getPlayer();
                    String levelStr = context.getOrDefault("level", "player");
                    handleInspect(player, levelStr);
                })
        );

        manager.command(builder.literal("tool")
                .permission("multiblockengine.tool")
                .handler(context -> {
                    if (!context.sender().isPlayer()) return;
                    Player player = context.sender().getPlayer();
                    // Fallback stub for /mbe tool if it was intended
                    sendMessage(player, MessageKey.of(ORIGIN, "commands.tool.not_implemented"), Map.of());
                })
        );
    }

    private void handleStatus(CommandSender sender) {
        MultiblockRuntimeService runtime = plugin.getManager();
        MetricsService metrics = runtime.getMetrics();
        
        sendMessage(sender, MSG_STATUS_TITLE, Map.of());
        sendMessage(sender, MSG_STATUS_LOADED_TYPES_VALUE, MessageUtils.params("value", runtime.getTypes().size()));
        sendMessage(sender, MSG_STATUS_TOTAL_CREATED_VALUE, MessageUtils.params("value", metrics.getCreatedInstances()));
        sendMessage(sender, MSG_STATUS_TOTAL_DESTROYED_VALUE, MessageUtils.params("value", metrics.getDestroyedInstances()));
        sendMessage(sender, MSG_STATUS_STRUCTURE_CHECKS_VALUE, MessageUtils.params("value", metrics.getStructureChecks()));
        sendMessage(sender, MSG_STATUS_AVG_TICK_VALUE, MessageUtils.params("value", String.format("%.1f s", metrics.getAverageTickTimeMs() / 1000.0D)));
    }

    private void handleReload(CommandSender sender) {
        sendMessage(sender, MSG_RELOAD_START, Map.of());
        
        plugin.reloadConfig();

        dev.darkblade.mbe.api.i18n.I18nService i18n = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.i18n.I18nService.class);
        if (i18n != null) {
            i18n.reload();
        }
        
        File multiblockDir = new File(plugin.getDataFolder(), "multiblocks");
        if (!multiblockDir.exists()) {
            multiblockDir.mkdirs();
        }
        
        MultiblockParser parser = plugin.getParser();
        List<MultiblockParser.LoadedType> loaded = parser.loadAllWithSources(multiblockDir);
        List<MultiblockType> newTypes = new ArrayList<>(loaded.size());
        java.util.Map<String, dev.darkblade.mbe.core.domain.MultiblockSource> sources = new java.util.HashMap<>();
        for (MultiblockParser.LoadedType lt : loaded) {
            if (lt == null || lt.type() == null) {
                continue;
            }
            newTypes.add(lt.type());
            sources.put(lt.type().id().toString(), lt.source());
        }

        plugin.getManager().reloadTypesWithSources(newTypes, sources);
        plugin.getManager().getMetrics().setEnabled(plugin.getConfig().getBoolean("metrics", true));
        
        sendMessage(sender, MSG_RELOAD_DONE_TYPES, MessageUtils.params("count", newTypes.size()));
        sendMessage(sender, MSG_RELOAD_DONE_RESTART, Map.of());
    }

    private void handleReport(CommandSender sender, String targetName, boolean console) {
        if (console && sender instanceof Player && !sender.hasPermission("multiblockengine.report.console")) {
            sendMessage(sender, CoreMessageKeys.COMMAND_NO_PERMISSION, Map.of());
            return;
        }

        Player target;
        if (targetName != null && !targetName.isBlank()) {
            target = org.bukkit.Bukkit.getPlayer(targetName);
            if (target == null) {
                sendMessage(sender, MSG_REPORT_PLAYER_NOT_FOUND, MessageUtils.params("player", targetName));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sendMessage(sender, MSG_REPORT_PLAYER_NOT_FOUND, MessageUtils.params("player", "<jugador>"));
            return;
        }

        if (plugin.getAssemblyCoordinator() == null) {
            sendMessage(sender, MSG_REPORT_NONE, Map.of());
            return;
        }

        AssemblyReport report = plugin.getAssemblyCoordinator().lastReport(target.getUniqueId()).orElse(null);
        if (report == null) {
            sendMessage(sender, MSG_REPORT_NONE, Map.of());
            return;
        }

        sendReport(sender, report);
        if (console) {
            sendReport(org.bukkit.Bukkit.getConsoleSender(), report);
        }
    }

    private void sendReport(CommandSender sender, AssemblyReport report) {
        sendMessage(sender, MSG_REPORT_TITLE, Map.of());
        sendMessage(sender, MSG_REPORT_RESULT, MessageUtils.params("result", report.success() ? "SUCCESS" : "FAILED"));
        if (report.reasonKey() != null && !report.reasonKey().isBlank()) {
            sendMessage(sender, MSG_REPORT_REASON, MessageUtils.params("reason", report.reasonKey()));
        }
        sendMessage(sender, MSG_REPORT_TRIGGER, MessageUtils.params("trigger", report.trigger()));
        sendMessage(sender, MSG_REPORT_MULTIBLOCK, MessageUtils.params("multiblock", report.multiblockId()));
        sendMessage(sender, MSG_REPORT_DEBUG_TITLE, Map.of());
        
        if (report.debugData().isEmpty()) {
            sendMessage(sender, MSG_REPORT_DEBUG_LINE, MessageUtils.params("key", "-", "value", "-"));
        } else {
            for (Map.Entry<String, Object> entry : report.debugData().entrySet()) {
                sendMessage(sender, MSG_REPORT_DEBUG_LINE, MessageUtils.params("key", entry.getKey(), "value", String.valueOf(entry.getValue())));
            }
        }
        sendMessage(sender, MSG_REPORT_TRACE_TITLE, Map.of());
        int i = 1;
        for (AssemblyStepTrace step : report.trace()) {
            if (step == null) {
                continue;
            }
            sendMessage(sender, MSG_REPORT_TRACE_LINE, MessageUtils.params("index", i++, "step", step.step(), "status", step.success() ? "OK" : "FAIL", "detail", step.detail()));
        }
    }

    private void handleUi(CommandSender sender) {
        dev.darkblade.mbe.api.ui.PanelViewService panelViewService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.ui.PanelViewService.class);
        if (panelViewService == null) {
            sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.unavailable"), Map.of());
            return;
        }
        List<String> panelIds = panelViewService.getRegisteredPanelIds();
        sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.title"), MessageUtils.params("count", panelIds.size()));
        if (panelIds.isEmpty()) {
            sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.none"), Map.of());
            return;
        }
        for (String panelId : panelIds) {
            sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.entry"), MessageUtils.params("panel", panelId));
        }
    }

    private void handleInspect(Player player, String levelStr) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.must_look_at_block"), Map.of());
            return;
        }

        MultiblockRuntimeService runtime = plugin.getManager();
        Optional<MultiblockInstance> instanceOpt = runtime.getInstanceAt(target.getLocation());

        if (instanceOpt.isEmpty()) {
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.none"), Map.of());
            return;
        }

        MultiblockInstance instance = instanceOpt.get();

        InspectionLevel requestedLevel = InspectionLevel.PLAYER;
        if (levelStr != null && !levelStr.isBlank()) {
            requestedLevel = switch (levelStr.toLowerCase(java.util.Locale.ROOT)) {
                case "player" -> InspectionLevel.PLAYER;
                case "operator", "op", "admin" -> InspectionLevel.OPERATOR;
                case "debug" -> InspectionLevel.DEBUG;
                case "internal" -> InspectionLevel.INTERNAL;
                default -> InspectionLevel.PLAYER;
            };
        }

        if (requestedLevel == InspectionLevel.OPERATOR && !player.hasPermission("multiblockengine.inspect.operator")) {
            requestedLevel = InspectionLevel.PLAYER;
        }
        if (requestedLevel == InspectionLevel.DEBUG && !player.hasPermission("multiblockengine.inspect.debug")) {
            requestedLevel = InspectionLevel.PLAYER;
        }
        if (requestedLevel == InspectionLevel.INTERNAL && !player.hasPermission("multiblockengine.inspect.internal")) {
            requestedLevel = InspectionLevel.PLAYER;
        }

        InspectionPipelineService pipeline = plugin.getAddonLifecycleService().getCoreService(InspectionPipelineService.class);
        if (pipeline != null) {
            Inspectable inspectable = ctx -> {
                java.util.Map<String, InspectionEntry> out = new java.util.LinkedHashMap<>();
                out.put("type", new InspectionEntry("type", instance.type() == null ? "" : instance.type().id().toString(), EntryType.TEXT, InspectionLevel.PLAYER));
                out.put("state", new InspectionEntry("state", instance.state() == null ? "" : instance.state().name(), EntryType.TEXT, InspectionLevel.PLAYER));
                out.put("facing", new InspectionEntry("facing", instance.facing() == null ? "" : instance.facing().name(), EntryType.TEXT, InspectionLevel.PLAYER));
                out.put("anchor", new InspectionEntry("anchor", formatLoc(instance.anchorLocation()), EntryType.TEXT, InspectionLevel.PLAYER));
                return new InspectionData(java.util.Map.copyOf(out));
            };

            InspectionRenderer renderer = (p, data, ctx) -> {
                sendMessage(p, MessageKey.of(ORIGIN, "commands.inspect.title"), Map.of());

                InspectionEntry type = data.entries().get("type");
                if (type != null) {
                    sendMessage(p, MessageKey.of(ORIGIN, "commands.inspect.type"), MessageUtils.params("value", String.valueOf(type.value())));
                }

                InspectionEntry state = data.entries().get("state");
                if (state != null) {
                    sendMessage(p, MessageKey.of(ORIGIN, "commands.inspect.state"), MessageUtils.params("value", String.valueOf(state.value())));
                }

                InspectionEntry facing = data.entries().get("facing");
                if (facing != null) {
                    sendMessage(p, MessageKey.of(ORIGIN, "commands.inspect.facing"), MessageUtils.params("value", String.valueOf(facing.value())));
                }

                InspectionEntry anchor = data.entries().get("anchor");
                if (anchor != null) {
                    sendMessage(p, MessageKey.of(ORIGIN, "commands.inspect.anchor"), MessageUtils.params("value", String.valueOf(anchor.value())));
                }
            };

            pipeline.inspect(player, InteractionSource.COMMAND, requestedLevel, inspectable, renderer);
        } else {
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.title"), Map.of());
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.type"), MessageUtils.params("value", instance.type().id().toString()));
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.state"), MessageUtils.params("value", String.valueOf(instance.state())));
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.facing"), MessageUtils.params("value", String.valueOf(instance.facing())));
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.anchor"), MessageUtils.params("value", formatLoc(instance.anchorLocation())));
        }
        
        highlightStructure(player, instance);
    }

    private void highlightStructure(Player player, MultiblockInstance instance) {
        Location anchor = instance.anchorLocation();
        BlockFace facing = instance.facing();
        
        for (PatternEntry entry : instance.type().pattern()) {
            Vector offset = rotateVector(entry.offset(), facing);
            Location loc = anchor.clone().add(offset);
            
            player.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
        }
        
        player.spawnParticle(Particle.FLAME, anchor.clone().add(0.5, 0.5, 0.5), 10, 0.1, 0.1, 0.1, 0.05);
        sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.highlighted"), Map.of());
    }

    private String formatLoc(Location loc) {
        if (loc == null) {
            return "";
        }
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
    
    private Vector rotateVector(Vector v, BlockFace facing) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        
        return switch (facing) {
            case NORTH -> new Vector(x, y, z);
            case EAST -> new Vector(-z, y, x);
            case SOUTH -> new Vector(-x, y, -z);
            case WEST -> new Vector(z, y, -x);
            default -> new Vector(x, y, z);
        };
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            org.bukkit.Bukkit.getLogger().info(key.fullKey() + " " + params.toString());
        }
    }
}
