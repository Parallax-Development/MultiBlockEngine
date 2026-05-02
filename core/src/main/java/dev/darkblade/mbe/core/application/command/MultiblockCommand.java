package dev.darkblade.mbe.core.application.command;

import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.blueprint.BlueprintItem;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MetricsService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.command.service.ServicesCommandRouter;
import dev.darkblade.mbe.core.application.command.service.impl.BlueprintCommandService;
import dev.darkblade.mbe.core.application.command.service.impl.AssemblyCommandService;
import dev.darkblade.mbe.core.application.command.service.impl.ItemsCommandService;
import dev.darkblade.mbe.core.application.command.service.impl.UiCommandService;
import dev.darkblade.mbe.catalog.StructureCatalogService;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.core.internal.tooling.export.ExportSession;
import dev.darkblade.mbe.core.internal.tooling.export.SelectionService;
import dev.darkblade.mbe.core.internal.tooling.export.StructureExporter;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.assembly.AssemblyStepTrace;
import dev.darkblade.mbe.api.service.EntryType;
import dev.darkblade.mbe.api.service.Inspectable;
import dev.darkblade.mbe.api.service.InspectionData;
import dev.darkblade.mbe.api.service.InspectionEntry;
import dev.darkblade.mbe.api.service.InspectionLevel;
import dev.darkblade.mbe.api.service.InspectionPipelineService;
import dev.darkblade.mbe.api.service.InspectionRenderer;
import dev.darkblade.mbe.api.service.InteractionSource;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.core.infrastructure.config.parser.MultiblockParser;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MultiblockCommand implements CommandExecutor, TabCompleter {

    private static final String ORIGIN = "mbe";

    private static final String PERM_USE = "multiblockengine.use";
    private static final String PERM_ADMIN = "multiblockengine.admin";
    private static final String PERM_DEBUG = "multiblockengine.debug";
    private static final String PERM_ADMIN_RELOAD = "multiblockengine.admin.reload";
    private static final String PERM_ADMIN_EXPORT = "multiblockengine.admin.export";
    private static final String PERM_ADMIN_SERVICES = "multiblockengine.admin.services";
    private static final String PERM_ADMIN_BLUEPRINT = "multiblockengine.admin.blueprint";
    private static final String PERM_DEBUG_SESSION = "multiblockengine.debug.session";
    private static final String PERM_DEBUG_SERVICES = "multiblockengine.debug.services";
    private static final String PERM_REPORT_CONSOLE = "mbe.debug.report.console";

    private static final MessageKey MSG_USAGE_CONSOLE = MessageKey.of(ORIGIN, "commands.usage.console");
    private static final MessageKey MSG_USAGE_PLAYER = MessageKey.of(ORIGIN, "commands.usage.player");
    private static final MessageKey MSG_UNKNOWN_SUBCOMMAND = MessageKey.of(ORIGIN, "commands.error.unknown_subcommand");
    private static final MessageKey MSG_DEBUG_USAGE = MessageKey.of(ORIGIN, "commands.debug.usage");
    private static final MessageKey MSG_DEBUG_TYPE_NOT_FOUND = MessageKey.of(ORIGIN, "commands.debug.type_not_found");
    private static final MessageKey MSG_DEBUG_PLAYER_NOT_FOUND = MessageKey.of(ORIGIN, "commands.debug.player_not_found");
    private static final MessageKey MSG_DEBUG_MUST_LOOK_AT_BLOCK = MessageKey.of(ORIGIN, "commands.debug.must_look_at_block");
    private static final MessageKey MSG_REPORT_NONE = MessageKey.of(ORIGIN, "commands.report.none");
    private static final MessageKey MSG_REPORT_TITLE = MessageKey.of(ORIGIN, "commands.report.title");
    private static final MessageKey MSG_REPORT_RESULT = MessageKey.of(ORIGIN, "commands.report.result");
    private static final MessageKey MSG_REPORT_TRIGGER = MessageKey.of(ORIGIN, "commands.report.trigger");
    private static final MessageKey MSG_REPORT_MULTIBLOCK = MessageKey.of(ORIGIN, "commands.report.multiblock");
    private static final MessageKey MSG_REPORT_REASON = MessageKey.of(ORIGIN, "commands.report.reason");
    private static final MessageKey MSG_REPORT_DEBUG_TITLE = MessageKey.of(ORIGIN, "commands.report.debug_title");
    private static final MessageKey MSG_REPORT_TRACE_TITLE = MessageKey.of(ORIGIN, "commands.report.trace_title");
    private static final MessageKey MSG_REPORT_TRACE_LINE = MessageKey.of(ORIGIN, "commands.report.trace_line");
    private static final MessageKey MSG_REPORT_DEBUG_LINE = MessageKey.of(ORIGIN, "commands.report.debug_line");
    private static final MessageKey MSG_REPORT_PLAYER_NOT_FOUND = MessageKey.of(ORIGIN, "commands.report.player_not_found");
    private static final MessageKey MSG_RELOAD_START = MessageKey.of(ORIGIN, "commands.reload.start");
    private static final MessageKey MSG_RELOAD_DONE_TYPES = MessageKey.of(ORIGIN, "commands.reload.done_types");
    private static final MessageKey MSG_RELOAD_DONE_RESTART = MessageKey.of(ORIGIN, "commands.reload.done_restart");
    private static final MessageKey MSG_STATUS_TITLE = MessageKey.of(ORIGIN, "commands.status.title");
    private static final MessageKey MSG_STATUS_LOADED_TYPES_VALUE = MessageKey.of(ORIGIN, "commands.status.loaded_types_value");
    private static final MessageKey MSG_STATUS_TOTAL_CREATED_VALUE = MessageKey.of(ORIGIN, "commands.status.total_created_value");
    private static final MessageKey MSG_STATUS_TOTAL_DESTROYED_VALUE = MessageKey.of(ORIGIN, "commands.status.total_destroyed_value");
    private static final MessageKey MSG_STATUS_STRUCTURE_CHECKS_VALUE = MessageKey.of(ORIGIN, "commands.status.structure_checks_value");
    private static final MessageKey MSG_STATUS_AVG_TICK_VALUE = MessageKey.of(ORIGIN, "commands.status.avg_tick_value");
    private static final MessageKey MSG_INSPECT_MUST_LOOK_AT_BLOCK = MessageKey.of(ORIGIN, "commands.inspect.must_look_at_block");
    private static final MessageKey MSG_INSPECT_NONE = MessageKey.of(ORIGIN, "commands.inspect.none_here");
    private static final MessageKey MSG_INSPECT_TITLE = MessageKey.of(ORIGIN, "commands.inspect.title");
    private static final MessageKey MSG_INSPECT_HIGHLIGHTED = MessageKey.of(ORIGIN, "commands.inspect.highlighted");
    private static final MessageKey MSG_INSPECT_TYPE_VALUE = MessageKey.of(ORIGIN, "commands.inspect.type_value");
    private static final MessageKey MSG_INSPECT_STATE_VALUE = MessageKey.of(ORIGIN, "commands.inspect.state_value");
    private static final MessageKey MSG_INSPECT_FACING_VALUE = MessageKey.of(ORIGIN, "commands.inspect.facing_value");
    private static final MessageKey MSG_INSPECT_ANCHOR_VALUE = MessageKey.of(ORIGIN, "commands.inspect.anchor_value");
    private static final MessageKey MSG_BLUEPRINT_AVAILABLE_IDS = MessageKey.of(ORIGIN, "commands.blueprint.available_ids");
    private static final MessageKey MSG_UI_DEBUG_USAGE = MessageKey.of(ORIGIN, "commands.ui.debug_usage");
    private static final MessageKey MSG_UI_DEBUG_TITLE = MessageKey.of(ORIGIN, "commands.ui.debug_title");
    private static final MessageKey MSG_UI_DEBUG_ENTRY = MessageKey.of(ORIGIN, "commands.ui.debug_entry");
    private static final MessageKey MSG_UI_DEBUG_NONE = MessageKey.of(ORIGIN, "commands.ui.debug_none");
    private static final MessageKey MSG_UI_DEBUG_UNAVAILABLE = MessageKey.of(ORIGIN, "commands.ui.debug_unavailable");

    private final MultiBlockEngine plugin;
    private final ServicesCommandRouter services;
    private final SelectionService exportSelections;
    private final StructureExporter structureExporter;
    private final BlueprintCommandService blueprintCommands;
    private final AssemblyCommandService assemblyCommands;

    public MultiblockCommand(MultiBlockEngine plugin, SelectionService exportSelections, StructureExporter structureExporter) {
        this.plugin = plugin;
        this.exportSelections = exportSelections;
        this.structureExporter = structureExporter;
        this.blueprintCommands = new BlueprintCommandService(plugin);
        this.assemblyCommands = new AssemblyCommandService(plugin);
        this.services = new ServicesCommandRouter(plugin);
        this.services.registerInternal(new ItemsCommandService(plugin));
        this.services.registerInternal(new UiCommandService(plugin));
        this.services.registerInternal(this.blueprintCommands);
        this.services.registerInternal(this.assemblyCommands);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;

        if (!sender.hasPermission(PERM_USE) && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
            return true;
        }

        if (safeArgs.length == 0 || safeArgs[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String root = safeArgs[0] == null ? "" : safeArgs[0].toLowerCase(Locale.ROOT);

        if (root.equals("services")) {
            if (!sender.hasPermission(PERM_ADMIN_SERVICES) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            return services.handle(sender, label, safeArgs);
        }

        if (root.equals("admin")) {
            if (!sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            return handleAdmin(sender, label, safeArgs);
        }

        if (root.equals("debug")) {
            if (!sender.hasPermission(PERM_DEBUG)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            return handleDebugLayer(sender, label, safeArgs);
        }

        if (root.equals("inspect")) {
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
                return true;
            }
            handleInspect(player, safeArgs);
            return true;
        }

        if (root.equals("status") || root.equals("stats")) {
            handleStatus(sender);
            return true;
        }

        if (root.equals("ui")) {
            return handleUi(sender, label, safeArgs);
        }

        if (root.equals("report")) {
            handleReport(sender, safeArgs);
            return true;
        }

        if (root.equals("assemble") || root.equals("form")) {
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
                return true;
            }
            assemblyCommands.executeAssemble(player);
            return true;
        }

        if (root.equals("disassemble") || root.equals("break")) {
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
                return true;
            }
            assemblyCommands.executeDisassemble(player);
            return true;
        }

        if (root.equals("reload")) {
            if (!sender.hasPermission(PERM_ADMIN_RELOAD) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            handleReload(sender);
            return true;
        }

        if (root.equals("export")) {
            if (!sender.hasPermission(PERM_ADMIN_EXPORT) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
                return true;
            }
            handleExport(player, label, safeArgs);
            return true;
        }

        if (root.equals("blueprint")) {
            if (!sender.hasPermission(PERM_ADMIN_BLUEPRINT) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            handleBlueprint(sender, label, safeArgs);
            return true;
        }

        send(sender, MSG_UNKNOWN_SUBCOMMAND);
        sendHelp(sender, label);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            send(sender, CoreMessageKeys.COMMAND_ADMIN_USAGE, "label", label);
            return true;
        }

        String op = args[1] == null ? "" : args[1].toLowerCase(Locale.ROOT);
        if (op.equals("reload")) {
            if (!sender.hasPermission(PERM_ADMIN_RELOAD) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            handleReload(sender);
            return true;
        }

        if (op.equals("export")) {
            if (!sender.hasPermission(PERM_ADMIN_EXPORT) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
                return true;
            }
            String[] forwarded = new String[Math.max(1, args.length - 1)];
            forwarded[0] = "export";
            if (args.length > 2) {
                System.arraycopy(args, 2, forwarded, 1, args.length - 2);
            }
            handleExport(player, label, forwarded);
            return true;
        }

        if (op.equals("services") || op.equals("service") || op.equals("list")) {
            if (!sender.hasPermission(PERM_ADMIN_SERVICES) && !sender.hasPermission(PERM_ADMIN)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            services.sendServicesListPublic(sender);
            return true;
        }

        if (!sender.hasPermission(PERM_ADMIN_SERVICES) && !sender.hasPermission(PERM_ADMIN)) {
            send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
            return true;
        }

        if (args.length >= 3 && "info".equalsIgnoreCase(args[2])) {
            List<String> remaining = args.length <= 3 ? List.of() : java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 3, args.length));
            return services.dispatch(sender, label, op, "info", remaining);
        }

        List<String> remaining = args.length <= 2 ? List.of() : java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 2, args.length));
        return services.dispatch(sender, label, op, "execute", remaining);
    }

    private boolean handleDebugLayer(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            send(sender, CoreMessageKeys.COMMAND_DEBUG_USAGE_LAYER, "label", label);
            return true;
        }

        String op = args[1] == null ? "" : args[1].toLowerCase(Locale.ROOT);
        if (op.equals("list") || op.equals("services") || op.equals("service")) {
            if (!sender.hasPermission(PERM_DEBUG_SERVICES) && !sender.hasPermission(PERM_DEBUG)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            services.sendServicesListPublic(sender);
            return true;
        }

        if (op.equals("type")) {
            if (!sender.hasPermission(PERM_DEBUG_SESSION) && !sender.hasPermission(PERM_DEBUG)) {
                send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
                return true;
            }
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
                return true;
            }
            String[] forwarded = new String[Math.max(1, args.length - 1)];
            forwarded[0] = "debug";
            if (args.length > 2) {
                System.arraycopy(args, 2, forwarded, 1, args.length - 2);
            }
            handleDebug(player, forwarded);
            return true;
        }

        if (sender.hasPermission(PERM_DEBUG_SESSION) && sender instanceof Player player) {
            if (plugin.getManager().getType(op).isPresent()) {
                handleDebug(player, args);
                return true;
            }
        }

        if (!sender.hasPermission(PERM_DEBUG_SERVICES) && !sender.hasPermission(PERM_DEBUG)) {
            send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
            return true;
        }

        if (args.length >= 3 && "info".equalsIgnoreCase(args[2])) {
            List<String> remaining = args.length <= 3 ? List.of() : java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 3, args.length));
            return services.dispatch(sender, label, op, "info", remaining);
        }

        List<String> remaining = args.length <= 2 ? List.of() : java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 2, args.length));
        return services.dispatch(sender, label, op, "execute", remaining);
    }

    private boolean handleUi(CommandSender sender, String label, String[] args) {
        if (args.length < 3 || !"debug".equalsIgnoreCase(args[1]) || !"panels".equalsIgnoreCase(args[2])) {
            send(sender, MSG_UI_DEBUG_USAGE, "label", label);
            return true;
        }
        PanelViewService panelViewService = plugin.getAddonLifecycleService().getCoreService(PanelViewService.class);
        if (panelViewService == null) {
            send(sender, MSG_UI_DEBUG_UNAVAILABLE);
            return true;
        }
        List<String> panelIds = panelViewService.getRegisteredPanelIds();
        send(sender, MSG_UI_DEBUG_TITLE, "count", panelIds.size());
        if (panelIds.isEmpty()) {
            send(sender, MSG_UI_DEBUG_NONE);
            return true;
        }
        for (String panelId : panelIds) {
            send(sender, MSG_UI_DEBUG_ENTRY, "panel", panelId);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        send(sender, CoreMessageKeys.COMMAND_HELP_INSPECT, "label", label);
        send(sender, CoreMessageKeys.COMMAND_HELP_ASSEMBLE, "label", label);
        send(sender, CoreMessageKeys.COMMAND_HELP_DISASSEMBLE, "label", label);
        send(sender, CoreMessageKeys.COMMAND_HELP_STATUS, "label", label);
        send(sender, CoreMessageKeys.COMMAND_HELP_REPORT, "label", label);
        if (sender.hasPermission(PERM_ADMIN_BLUEPRINT) || sender.hasPermission(PERM_ADMIN)) {
            send(sender, CoreMessageKeys.COMMAND_HELP_BLUEPRINT_CATALOG, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_BLUEPRINT_LIST, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_BLUEPRINT_GIVE, "label", label);
        }

        if (sender.hasPermission(PERM_ADMIN)) {
            send(sender, CoreMessageKeys.COMMAND_HELP_ADMIN_RELOAD, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_ADMIN_EXPORT, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_ADMIN_SERVICES, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_ADMIN_SERVICE, "label", label);
        }
        if (sender.hasPermission(PERM_DEBUG)) {
            send(sender, CoreMessageKeys.COMMAND_HELP_DEBUG_TYPE, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_DEBUG_SERVICE, "label", label);
            send(sender, CoreMessageKeys.COMMAND_HELP_DEBUG_LIST, "label", label);
        }
    }

    

    private void handleExport(Player player, String label, String[] args) {
        if (args.length < 2) {
            send(player, CoreMessageKeys.EXPORT_USAGE, "label", label);
            return;
        }

        String op = args[1] == null ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (op) {
            case "start" -> {
                exportSelections.start(player);
                send(player, CoreMessageKeys.EXPORT_STARTED);
            }
            case "cancel" -> {
                boolean cancelled = exportSelections.cancel(player);
                send(player, cancelled ? CoreMessageKeys.EXPORT_CANCELLED : CoreMessageKeys.EXPORT_NO_ACTIVE);
            }
            case "pos1" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                Block b = player.getTargetBlockExact(10);
                if (b == null) {
                    send(player, CoreMessageKeys.EXPORT_MUST_LOOK);
                    return;
                }
                s.setPos1(b.getLocation());
                send(player, CoreMessageKeys.EXPORT_POS1_SET, "x", b.getX(), "y", b.getY(), "z", b.getZ());
            }
            case "pos2" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                Block b = player.getTargetBlockExact(10);
                if (b == null) {
                    send(player, CoreMessageKeys.EXPORT_MUST_LOOK);
                    return;
                }
                s.setPos2(b.getLocation());
                send(player, CoreMessageKeys.EXPORT_POS2_SET, "x", b.getX(), "y", b.getY(), "z", b.getZ());
            }
            case "mark" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                if (args.length < 3) {
                    send(player, CoreMessageKeys.EXPORT_MARK_USAGE, "label", label);
                    return;
                }
                String role = args[2];
                s.setPendingRole(role);
                send(player, CoreMessageKeys.EXPORT_MARK_PROMPT, "role", role);
            }
            case "save" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                if (args.length < 3) {
                    send(player, CoreMessageKeys.EXPORT_SAVE_USAGE, "label", label);
                    return;
                }
                String id = args[2];
                try {
                    var res = structureExporter.exportToFile(id, s, plugin.getDataFolder().toPath().resolve("exports"));
                    send(player, CoreMessageKeys.EXPORT_SAVE_OK, "id", res.id(), "blocks", res.blocks());
                    if (!res.warnings().isEmpty()) {
                        send(player, CoreMessageKeys.EXPORT_SAVE_WARNINGS, "count", res.warnings().size());
                    }
                } catch (StructureExporter.ExportException e) {
                    send(player, CoreMessageKeys.EXPORT_SAVE_ERROR, "error", e.getMessage());
                }
            }
            default -> send(player, CoreMessageKeys.EXPORT_SUBCOMMAND_INVALID, "label", label);
        }
    }

    private void handleBlueprint(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            send(sender, CoreMessageKeys.BLUEPRINT_USAGE, "label", label);
            return;
        }
        String op = args[1] == null ? "" : args[1].trim().toLowerCase(Locale.ROOT);
        if ("catalog".equalsIgnoreCase(op)) {
            if (!(sender instanceof Player player)) {
                send(sender, CoreMessageKeys.BLUEPRINT_PLAYER_ONLY_CATALOG);
                return;
            }
            blueprintCommands.openCatalog(player);
            return;
        }
        StructureCatalogService catalogService = plugin.getAddonLifecycleService().getCoreService(StructureCatalogService.class);
        ItemService itemService = plugin.getAddonLifecycleService().getCoreService(ItemService.class);
        ItemStackBridge itemStackBridge = plugin.getAddonLifecycleService().getCoreService(ItemStackBridge.class);
        if (catalogService == null || itemService == null || itemStackBridge == null) {
            send(sender, CoreMessageKeys.BLUEPRINT_SERVICES_UNAVAILABLE);
            return;
        }

        if ("list".equalsIgnoreCase(op)) {
            List<String> ids = new ArrayList<>();
            for (dev.darkblade.mbe.preview.MultiblockDefinition definition : catalogService.getAll()) {
                if (definition == null || definition.id() == null || definition.id().isBlank()) {
                    continue;
                }
                ids.add(definition.id());
            }
            if (ids.isEmpty()) {
                send(sender, CoreMessageKeys.BLUEPRINT_NONE_LOADED);
                return;
            }
            java.util.Collections.sort(ids);
            send(sender, CoreMessageKeys.BLUEPRINT_AVAILABLE_TITLE, "count", ids.size());
            send(sender, MSG_BLUEPRINT_AVAILABLE_IDS, "ids", String.join(", ", ids));
            return;
        }

        if (!"give".equalsIgnoreCase(op)) {
            send(sender, CoreMessageKeys.BLUEPRINT_USAGE, "label", label);
            return;
        }
        if (args.length < 3) {
            send(sender, CoreMessageKeys.COMMAND_HELP_BLUEPRINT_GIVE, "label", label);
            return;
        }
        String id = args[2] == null ? "" : args[2].trim();
        if (id.isBlank()) {
            send(sender, CoreMessageKeys.BLUEPRINT_ID_REQUIRED);
            return;
        }

        Player receiver;
        if (args.length >= 4 && args[3] != null && !args[3].isBlank()) {
            receiver = org.bukkit.Bukkit.getPlayer(args[3]);
            if (receiver == null) {
                send(sender, CoreMessageKeys.BLUEPRINT_PLAYER_NOT_FOUND, "player", args[3]);
                return;
            }
        } else if (sender instanceof Player playerSender) {
            receiver = playerSender;
        } else {
            send(sender, CoreMessageKeys.BLUEPRINT_CONSOLE_NEEDS_PLAYER, "label", label);
            return;
        }

        dev.darkblade.mbe.preview.MultiblockDefinition definition = null;
        for (dev.darkblade.mbe.preview.MultiblockDefinition candidate : catalogService.getAll()) {
            if (candidate == null || candidate.id() == null) {
                continue;
            }
            if (candidate.id().equalsIgnoreCase(id)) {
                definition = candidate;
                break;
            }
        }
        if (definition == null) {
            send(sender, CoreMessageKeys.BLUEPRINT_NOT_FOUND, "id", id);
            return;
        }
        org.bukkit.inventory.ItemStack blueprint = BlueprintItem.create(itemService, itemStackBridge, definition, receiver);
        if (blueprint == null) {
            send(sender, CoreMessageKeys.BLUEPRINT_CREATE_FAILED, "id", definition.id());
            return;
        }
        receiver.getInventory().addItem(blueprint);
        send(receiver, CoreMessageKeys.BLUEPRINT_RECEIVED, "id", definition.id());
        if (sender != receiver) {
            send(sender, CoreMessageKeys.BLUEPRINT_GIVEN, "player", receiver.getName(), "id", definition.id());
        }
    }

    private ExportSession ensureSession(Player player) {
        ExportSession s = exportSelections.session(player);
        if (s == null) {
            send(player, CoreMessageKeys.EXPORT_NO_ACTIVE);
        }
        return s;
    }

    private void handleDebug(Player player, String[] args) {
        if (args.length < 2) {
            send(player, MSG_DEBUG_USAGE);
            return;
        }
        
        String id = args[1];
        Optional<MultiblockType> typeOpt = plugin.getManager().getType(id);
        
        if (typeOpt.isEmpty()) {
            send(player, MSG_DEBUG_TYPE_NOT_FOUND, "id", id);
            return;
        }
        MultiblockType type = typeOpt.get();

        plugin.getManager().getSource(type.id()).ifPresent(src -> {
            send(player, CoreMessageKeys.DEBUG_SOURCE, "sourceType", src.type().name(), "path", src.path());
        });
        send(player, CoreMessageKeys.DEBUG_SIGNATURE, "signature", plugin.getManager().signatureOf(type));
        
        // Target player
        Player targetPlayer = player;
        if (args.length >= 3) {
            targetPlayer = org.bukkit.Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                send(player, MSG_DEBUG_PLAYER_NOT_FOUND, "player", args[2]);
                return;
            }
        }
        
        // Raytrace for anchor
        Block targetBlock = targetPlayer.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            send(player, MSG_DEBUG_MUST_LOOK_AT_BLOCK);
            return;
        }
        
        // Start session
        plugin.getDebugSessionService().startSession(targetPlayer, type, targetBlock.getLocation());
    }

    private void handleReport(CommandSender sender, String[] args) {
        boolean printInConsole = false;
        String targetName = null;
        if (args != null) {
            for (int i = 1; i < args.length; i++) {
                String raw = args[i];
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                if ("--console".equalsIgnoreCase(raw)) {
                    printInConsole = true;
                    continue;
                }
                if (targetName == null) {
                    targetName = raw;
                }
            }
        }

        if (printInConsole && sender instanceof Player && !sender.hasPermission(PERM_REPORT_CONSOLE)) {
            send(sender, CoreMessageKeys.COMMAND_NO_PERMISSION);
            return;
        }

        Player target;
        if (targetName != null && !targetName.isBlank()) {
            target = org.bukkit.Bukkit.getPlayer(targetName);
            if (target == null) {
                send(sender, MSG_REPORT_PLAYER_NOT_FOUND, "player", targetName);
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            send(sender, MSG_REPORT_PLAYER_NOT_FOUND, "player", "<jugador>");
            return;
        }

        if (plugin.getAssemblyCoordinator() == null) {
            send(sender, MSG_REPORT_NONE);
            return;
        }

        AssemblyReport report = plugin.getAssemblyCoordinator().lastReport(target.getUniqueId()).orElse(null);
        if (report == null) {
            send(sender, MSG_REPORT_NONE);
            return;
        }

        sendReport(sender, report);
        if (printInConsole) {
            sendReport(org.bukkit.Bukkit.getConsoleSender(), report);
        }
    }

    private void sendReport(CommandSender sender, AssemblyReport report) {
        send(sender, MSG_REPORT_TITLE);
        send(sender, MSG_REPORT_RESULT, "result", report.success() ? "SUCCESS" : "FAILED");
        if (report.reasonKey() != null && !report.reasonKey().isBlank()) {
            send(sender, MSG_REPORT_REASON, "reason", report.reasonKey());
        }
        send(sender, MSG_REPORT_TRIGGER, "trigger", report.trigger());
        send(sender, MSG_REPORT_MULTIBLOCK, "multiblock", report.multiblockId());
        send(sender, MSG_REPORT_DEBUG_TITLE);
        if (report.debugData().isEmpty()) {
            send(sender, MSG_REPORT_DEBUG_LINE, "key", "-", "value", "-");
        } else {
            for (Map.Entry<String, Object> entry : report.debugData().entrySet()) {
                send(sender, MSG_REPORT_DEBUG_LINE, "key", entry.getKey(), "value", String.valueOf(entry.getValue()));
            }
        }
        send(sender, MSG_REPORT_TRACE_TITLE);
        int i = 1;
        for (AssemblyStepTrace step : report.trace()) {
            if (step == null) {
                continue;
            }
            send(
                    sender,
                    MSG_REPORT_TRACE_LINE,
                    "index", i++,
                    "step", step.step(),
                    "status", step.success() ? "OK" : "FAIL",
                    "detail", step.detail()
            );
        }
    }

    private void handleReload(CommandSender sender) {
        send(sender, MSG_RELOAD_START);
        
        // Reload Config
        plugin.reloadConfig();

        I18nService i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        if (i18n != null) {
            i18n.reload();
        }
        
        // Reload Types
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
            sources.put(lt.type().id(), lt.source());
        }

        plugin.getManager().reloadTypesWithSources(newTypes, sources);
        plugin.getManager().getMetrics().setEnabled(plugin.getConfig().getBoolean("metrics", true));
        
        send(sender, MSG_RELOAD_DONE_TYPES, "count", newTypes.size());
        send(sender, MSG_RELOAD_DONE_RESTART);
    }
    
    private void handleStatus(CommandSender sender) {
        MultiblockRuntimeService manager = plugin.getManager();
        MetricsService metrics = manager.getMetrics();
        
        send(sender, MSG_STATUS_TITLE);
        send(sender, MSG_STATUS_LOADED_TYPES_VALUE, "value", manager.getTypes().size());
        send(sender, MSG_STATUS_TOTAL_CREATED_VALUE, "value", metrics.getCreatedInstances());
        send(sender, MSG_STATUS_TOTAL_DESTROYED_VALUE, "value", metrics.getDestroyedInstances());
        send(sender, MSG_STATUS_STRUCTURE_CHECKS_VALUE, "value", metrics.getStructureChecks());
        send(sender, MSG_STATUS_AVG_TICK_VALUE, "value", String.format("%.1f s", metrics.getAverageTickTimeMs() / 1000.0D));
    }

    private void handleAssemble(Player player) {
        if (plugin.getAssemblyCoordinator() == null) {
            send(player, CoreMessageKeys.ASSEMBLY_COORDINATOR_UNAVAILABLE);
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            send(player, CoreMessageKeys.ASSEMBLE_MUST_LOOK);
            return;
        }

        dev.darkblade.mbe.api.assembly.AssemblyContext ctx = new dev.darkblade.mbe.api.assembly.AssemblyContext(
                player,
                target,
                new dev.darkblade.mbe.api.service.interaction.InteractionIntent(
                        player,
                        dev.darkblade.mbe.api.service.interaction.InteractionType.PROGRAMMATIC,
                        target,
                        null,
                        dev.darkblade.mbe.api.service.interaction.InteractionSource.PROGRAMMATIC
                )
        );

        AssemblyReport report = plugin.getAssemblyCoordinator().tryAssembleAt(target, ctx);
        if (report == null) {
            send(player, CoreMessageKeys.ASSEMBLE_TRY_FAILED);
            return;
        }

        if (report.result() == AssemblyReport.Result.SUCCESS) {
            send(player, CoreMessageKeys.ASSEMBLE_OK, "id", report.multiblockId());
        } else {
            String reason = report.failureReason() == null || report.failureReason().isBlank() ? report.result().name() : report.failureReason();
            send(player, CoreMessageKeys.ASSEMBLE_FAILED, "reason", reason);
        }
    }

    private void handleDisassemble(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            send(player, CoreMessageKeys.DISASSEMBLE_MUST_LOOK);
            return;
        }

        Optional<MultiblockInstance> instanceOpt = plugin.getManager().getInstanceAt(target.getLocation());
        if (instanceOpt.isEmpty()) {
            send(player, CoreMessageKeys.DISASSEMBLE_NONE_HERE);
            return;
        }

        MultiblockInstance instance = instanceOpt.get();
        dev.darkblade.mbe.api.event.MultiblockBreakEvent mbEvent = new dev.darkblade.mbe.api.event.MultiblockBreakEvent(instance, player);
        org.bukkit.Bukkit.getPluginManager().callEvent(mbEvent);
        if (mbEvent.isCancelled()) {
            send(player, CoreMessageKeys.ACTION_CANCELLED);
            return;
        }

        for (dev.darkblade.mbe.core.domain.action.Action a : instance.type().onBreakActions()) {
            try {
                if (a != null) {
                    a.execute(instance, player);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Break action failed: " + (a == null ? "<null>" : a.getClass().getSimpleName()), t);
            }
        }
        plugin.getManager().destroyInstance(instance);
        send(player, CoreMessageKeys.DISASSEMBLED);
    }

    private void handleInspect(Player player, String[] args) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            send(player, MSG_INSPECT_MUST_LOOK_AT_BLOCK);
            return;
        }

        MultiblockRuntimeService manager = plugin.getManager();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(target.getLocation());

        if (instanceOpt.isEmpty()) {
            send(player, MSG_INSPECT_NONE);
            return;
        }

        MultiblockInstance instance = instanceOpt.get();

        InspectionLevel requestedLevel = null;
        if (args != null && args.length >= 2 && args[1] != null) {
            String s = args[1].trim().toLowerCase(Locale.ROOT);
            if (!s.isBlank()) {
                requestedLevel = switch (s) {
                    case "player" -> InspectionLevel.PLAYER;
                    case "operator", "op", "admin" -> InspectionLevel.OPERATOR;
                    case "debug" -> InspectionLevel.DEBUG;
                    case "internal" -> InspectionLevel.INTERNAL;
                    default -> null;
                };
            }
        }

        if (requestedLevel == InspectionLevel.OPERATOR && !player.hasPermission("mbe.inspect.operator")) {
            requestedLevel = InspectionLevel.PLAYER;
        }
        if (requestedLevel == InspectionLevel.DEBUG && !player.hasPermission("mbe.inspect.debug")) {
            requestedLevel = InspectionLevel.PLAYER;
        }
        if (requestedLevel == InspectionLevel.INTERNAL && !player.hasPermission("mbe.inspect.internal")) {
            requestedLevel = InspectionLevel.PLAYER;
        }

        InspectionPipelineService pipeline = plugin.getAddonLifecycleService().getCoreService(InspectionPipelineService.class);
        if (pipeline != null) {
            Inspectable inspectable = ctx -> {
                java.util.Map<String, InspectionEntry> out = new java.util.LinkedHashMap<>();
                out.put("type", new InspectionEntry("type", instance.type() == null ? "" : instance.type().id(), EntryType.TEXT, InspectionLevel.PLAYER));
                out.put("state", new InspectionEntry("state", instance.state() == null ? "" : instance.state().name(), EntryType.TEXT, InspectionLevel.PLAYER));
                out.put("facing", new InspectionEntry("facing", instance.facing() == null ? "" : instance.facing().name(), EntryType.TEXT, InspectionLevel.PLAYER));
                out.put("anchor", new InspectionEntry("anchor", formatLoc(instance.anchorLocation()), EntryType.TEXT, InspectionLevel.PLAYER));
                return new InspectionData(java.util.Map.copyOf(out));
            };

            InspectionRenderer renderer = (p, data, ctx) -> {
                send(p, MSG_INSPECT_TITLE);

                InspectionEntry type = data.entries().get("type");
                if (type != null) {
                    send(p, MSG_INSPECT_TYPE_VALUE, "value", String.valueOf(type.value()));
                }

                InspectionEntry state = data.entries().get("state");
                if (state != null) {
                    send(p, MSG_INSPECT_STATE_VALUE, "value", String.valueOf(state.value()));
                }

                InspectionEntry facing = data.entries().get("facing");
                if (facing != null) {
                    send(p, MSG_INSPECT_FACING_VALUE, "value", String.valueOf(facing.value()));
                }

                InspectionEntry anchor = data.entries().get("anchor");
                if (anchor != null) {
                    send(p, MSG_INSPECT_ANCHOR_VALUE, "value", String.valueOf(anchor.value()));
                }
            };

            pipeline.inspect(player, InteractionSource.COMMAND, requestedLevel, inspectable, renderer);
        } else {
            send(player, MSG_INSPECT_TITLE);
            send(player, MSG_INSPECT_TYPE_VALUE, "value", instance.type().id());
            send(player, MSG_INSPECT_STATE_VALUE, "value", String.valueOf(instance.state()));
            send(player, MSG_INSPECT_FACING_VALUE, "value", String.valueOf(instance.facing()));
            send(player, MSG_INSPECT_ANCHOR_VALUE, "value", formatLoc(instance.anchorLocation()));
        }
        
        // Visualize
        highlightStructure(player, instance);
    }
    
    private void highlightStructure(Player player, MultiblockInstance instance) {
        // Simple particle visualization
        // Show particles at every block of the structure
        Location anchor = instance.anchorLocation();
        BlockFace facing = instance.facing();
        
        // We need access to rotateVector, but it is private in Manager.
        // We should probably expose a helper or duplicate logic.
        // For now, let's just duplicate logic or make it public static in Manager?
        // Let's duplicate for simplicity or check if we can make it public.
        // Or better, let's iterate the pattern and calculate.
        
        for (PatternEntry entry : instance.type().pattern()) {
            Vector offset = rotateVector(entry.offset(), facing);
            Location loc = anchor.clone().add(offset);
            
            player.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
        }
        
        // Highlight anchor specifically
        player.spawnParticle(Particle.FLAME, anchor.clone().add(0.5, 0.5, 0.5), 10, 0.1, 0.1, 0.1, 0.05);
        
        send(player, MSG_INSPECT_HIGHLIGHTED);
    }

    private void send(CommandSender sender, MessageKey key) {
        if (sender == null || key == null) {
            return;
        }
        PlayerMessageService messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of()));
            return;
        }
        I18nService i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        if (i18n == null) {
            return;
        }
        i18n.send(sender, key);
    }

    private void send(CommandSender sender, MessageKey key, Object... params) {
        if (sender == null || key == null) {
            return;
        }
        PlayerMessageService messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(
                    key,
                    MessageChannel.CHAT,
                    MessagePriority.NORMAL,
                    castParams(MessageUtils.params(params))
            ));
            return;
        }
        I18nService i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        if (i18n == null) {
            return;
        }
        i18n.send(sender, key, MessageUtils.params(params));
    }

    private Map<String, Object> castParams(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new java.util.HashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private String formatLoc(Location loc) {
        if (loc == null) {
            return "";
        }
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
    
    // Duplicate from Manager for now
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;

        if (safeArgs.length > 0 && safeArgs[0] != null && safeArgs[0].equalsIgnoreCase("services")) {
            return services.tabComplete(sender, safeArgs);
        }

        if (safeArgs.length == 1) {
            List<String> roots = new ArrayList<>();
            roots.add("help");
            roots.add("inspect");
            roots.add("status");
            roots.add("stats");
            roots.add("ui");
            roots.add("report");
            if (sender instanceof Player) {
                roots.add("assemble");
                roots.add("disassemble");
            }
            if (sender.hasPermission(PERM_ADMIN_EXPORT) || sender.hasPermission(PERM_ADMIN)) {
                roots.add("export");
            }
            if (sender.hasPermission(PERM_ADMIN_BLUEPRINT) || sender.hasPermission(PERM_ADMIN)) {
                roots.add("blueprint");
            }
            if (sender.hasPermission(PERM_ADMIN_RELOAD) || sender.hasPermission(PERM_ADMIN)) {
                roots.add("reload");
            }
            if (sender.hasPermission(PERM_ADMIN_SERVICES) || sender.hasPermission(PERM_ADMIN)) {
                roots.add("services");
                roots.add("admin");
            }
            if (sender.hasPermission(PERM_DEBUG)) {
                roots.add("debug");
            }
            return filter(roots, safeArgs[0]);
        }

        String root = safeArgs[0] == null ? "" : safeArgs[0].toLowerCase(Locale.ROOT);

        if (safeArgs.length == 2 && root.equals("inspect")) {
            return filter(List.of("player", "operator", "debug", "internal"), safeArgs[1]);
        }

        if (root.equals("report")) {
            if (safeArgs.length == 2) {
                List<String> out = new ArrayList<>();
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (player == null || player.getName() == null || player.getName().isBlank()) {
                        continue;
                    }
                    out.add(player.getName());
                }
                if (sender instanceof Player && sender.hasPermission(PERM_REPORT_CONSOLE)) {
                    out.add("--console");
                }
                return filter(out, safeArgs[1]);
            }
            if (safeArgs.length == 3) {
                if (!(sender instanceof Player) || !sender.hasPermission(PERM_REPORT_CONSOLE)) {
                    return Collections.emptyList();
                }
                if ("--console".equalsIgnoreCase(safeArgs[1])) {
                    List<String> out = new ArrayList<>();
                    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (player == null || player.getName() == null || player.getName().isBlank()) {
                            continue;
                        }
                        out.add(player.getName());
                    }
                    return filter(out, safeArgs[2]);
                }
                return filter(List.of("--console"), safeArgs[2]);
            }
            return Collections.emptyList();
        }

        if (root.equals("ui")) {
            if (safeArgs.length == 2) {
                return filter(List.of("debug"), safeArgs[1]);
            }
            if (safeArgs.length == 3 && "debug".equalsIgnoreCase(safeArgs[1])) {
                return filter(List.of("panels"), safeArgs[2]);
            }
            return Collections.emptyList();
        }

        if (root.equals("export")) {
            if (safeArgs.length == 2) {
                return filter(List.of("start", "pos1", "pos2", "mark", "save", "cancel"), safeArgs[1]);
            }
            if (safeArgs.length == 3 && "mark".equalsIgnoreCase(safeArgs[1])) {
                return filter(List.of("controller", "input", "output", "decorative"), safeArgs[2]);
            }
        }

        if (root.equals("blueprint")) {
            if (safeArgs.length == 2) {
                return filter(List.of("catalog", "give", "list"), safeArgs[1]);
            }
            if (safeArgs.length == 3 && "give".equalsIgnoreCase(safeArgs[1])) {
                List<String> ids = new ArrayList<>();
                for (MultiblockType type : plugin.getManager().getTypes()) {
                    if (type == null || type.id() == null || type.id().isBlank()) {
                        continue;
                    }
                    ids.add(type.id());
                }
                return filter(ids, safeArgs[2]);
            }
            if (safeArgs.length == 4 && "give".equalsIgnoreCase(safeArgs[1])) {
                List<String> players = new ArrayList<>();
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (player == null || player.getName() == null || player.getName().isBlank()) {
                        continue;
                    }
                    players.add(player.getName());
                }
                return filter(players, safeArgs[3]);
            }
        }

        if (root.equals("admin")) {
            if (safeArgs.length == 2) {
                List<String> opts = new ArrayList<>();
                opts.add("reload");
                opts.add("export");
                opts.add("services");
                opts.addAll(services.serviceIds());
                return filter(opts, safeArgs[1]);
            }

            if (safeArgs.length == 3) {
                String op = safeArgs[1] == null ? "" : safeArgs[1].toLowerCase(Locale.ROOT);
                if (op.equals("export")) {
                    return filter(List.of("start", "pos1", "pos2", "mark", "save", "cancel"), safeArgs[2]);
                }
                if (op.equals("reload") || op.equals("services")) {
                    return Collections.emptyList();
                }
                return filter(List.of("info"), safeArgs[2]);
            }

            if (safeArgs.length >= 4) {
                String serviceId = safeArgs[1];
                String maybeInfo = safeArgs[2];
                if ("info".equalsIgnoreCase(maybeInfo)) {
                    return Collections.emptyList();
                }
                List<String> remaining = java.util.Arrays.asList(java.util.Arrays.copyOfRange(safeArgs, 2, safeArgs.length));
                String mode = remaining.size() >= 1 && "info".equalsIgnoreCase(remaining.get(0)) ? "info" : "execute";
                List<String> argsTail = remaining.size() >= 1 && "info".equalsIgnoreCase(remaining.get(0))
                        ? remaining.subList(1, remaining.size())
                        : remaining;
                return services.tabCompleteService(sender, serviceId, mode, argsTail);
            }
        }

        if (root.equals("debug")) {
            if (safeArgs.length == 2) {
                List<String> opts = new ArrayList<>();
                opts.add("type");
                opts.add("list");
                opts.addAll(services.serviceIds());
                List<String> types = new ArrayList<>();
                for (MultiblockType t : plugin.getManager().getTypes()) {
                    if (t != null && t.id() != null) {
                        types.add(t.id());
                    }
                }
                opts.addAll(types);
                return filter(opts, safeArgs[1]);
            }

            if (safeArgs.length == 3) {
                String op = safeArgs[1] == null ? "" : safeArgs[1].toLowerCase(Locale.ROOT);
                if (op.equals("type")) {
                    List<String> types = new ArrayList<>();
                    for (MultiblockType t : plugin.getManager().getTypes()) {
                        if (t != null && t.id() != null) {
                            types.add(t.id());
                        }
                    }
                    return filter(types, safeArgs[2]);
                }

                if (op.equals("list")) {
                    return Collections.emptyList();
                }

                return filter(List.of("info"), safeArgs[2]);
            }

            if (safeArgs.length == 4 && "type".equalsIgnoreCase(safeArgs[1])) {
                return null;
            }

            if (safeArgs.length >= 4) {
                String serviceId = safeArgs[1];
                String maybeInfo = safeArgs[2];
                if ("info".equalsIgnoreCase(maybeInfo)) {
                    return Collections.emptyList();
                }
                List<String> remaining = java.util.Arrays.asList(java.util.Arrays.copyOfRange(safeArgs, 2, safeArgs.length));
                String mode = remaining.size() >= 1 && "info".equalsIgnoreCase(remaining.get(0)) ? "info" : "execute";
                List<String> argsTail = remaining.size() >= 1 && "info".equalsIgnoreCase(remaining.get(0))
                        ? remaining.subList(1, remaining.size())
                        : remaining;
                return services.tabCompleteService(sender, serviceId, mode, argsTail);
            }
        }

        return Collections.emptyList();
    }
    
    private List<String> filter(List<String> list, String input) {
        List<String> filtered = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(s);
            }
        }
        Collections.sort(filtered);
        return filtered;
    }
}
