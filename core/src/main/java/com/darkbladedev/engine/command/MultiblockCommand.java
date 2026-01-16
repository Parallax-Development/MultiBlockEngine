package com.darkbladedev.engine.command;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.manager.MetricsManager;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.command.services.ServicesCommandRouter;
import com.darkbladedev.engine.command.services.impl.ItemsCommandService;
import com.darkbladedev.engine.command.services.impl.UiCommandService;
import com.darkbladedev.engine.api.i18n.I18nService;
import com.darkbladedev.engine.api.i18n.MessageKey;
import com.darkbladedev.engine.export.ExportSession;
import com.darkbladedev.engine.export.SelectionManager;
import com.darkbladedev.engine.export.StructureExporter;
import com.darkbladedev.engine.api.assembly.AssemblyReport;
import com.darkbladedev.engine.api.inspection.EntryType;
import com.darkbladedev.engine.api.inspection.Inspectable;
import com.darkbladedev.engine.api.inspection.InspectionData;
import com.darkbladedev.engine.api.inspection.InspectionEntry;
import com.darkbladedev.engine.api.inspection.InspectionLevel;
import com.darkbladedev.engine.api.inspection.InspectionPipelineService;
import com.darkbladedev.engine.api.inspection.InspectionRenderer;
import com.darkbladedev.engine.api.inspection.InteractionSource;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.parser.MultiblockParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.Optional;

public class MultiblockCommand implements CommandExecutor, TabCompleter {

    private static final String ORIGIN = "mbe";

    private static final String PERM_USE = "multiblockengine.use";
    private static final String PERM_ADMIN = "multiblockengine.admin";
    private static final String PERM_DEBUG = "multiblockengine.debug";
    private static final String PERM_ADMIN_RELOAD = "multiblockengine.admin.reload";
    private static final String PERM_ADMIN_EXPORT = "multiblockengine.admin.export";
    private static final String PERM_ADMIN_SERVICES = "multiblockengine.admin.services";
    private static final String PERM_DEBUG_SESSION = "multiblockengine.debug.session";
    private static final String PERM_DEBUG_SERVICES = "multiblockengine.debug.services";

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
    private static final MessageKey MSG_REPORT_PLAYER_NOT_FOUND = MessageKey.of(ORIGIN, "commands.report.player_not_found");
    private static final MessageKey MSG_RELOAD_START = MessageKey.of(ORIGIN, "commands.reload.start");
    private static final MessageKey MSG_RELOAD_DONE_TYPES = MessageKey.of(ORIGIN, "commands.reload.done_types");
    private static final MessageKey MSG_RELOAD_DONE_RESTART = MessageKey.of(ORIGIN, "commands.reload.done_restart");
    private static final MessageKey MSG_STATUS_TITLE = MessageKey.of(ORIGIN, "commands.status.title");
    private static final MessageKey MSG_STATUS_LOADED_TYPES = MessageKey.of(ORIGIN, "commands.status.loaded_types");
    private static final MessageKey MSG_STATUS_TOTAL_CREATED = MessageKey.of(ORIGIN, "commands.status.total_created");
    private static final MessageKey MSG_STATUS_TOTAL_DESTROYED = MessageKey.of(ORIGIN, "commands.status.total_destroyed");
    private static final MessageKey MSG_STATUS_STRUCTURE_CHECKS = MessageKey.of(ORIGIN, "commands.status.structure_checks");
    private static final MessageKey MSG_STATUS_AVG_TICK = MessageKey.of(ORIGIN, "commands.status.avg_tick");
    private static final MessageKey MSG_INSPECT_MUST_LOOK_AT_BLOCK = MessageKey.of(ORIGIN, "commands.inspect.must_look_at_block");
    private static final MessageKey MSG_INSPECT_NONE = MessageKey.of(ORIGIN, "commands.inspect.none_here");
    private static final MessageKey MSG_INSPECT_TITLE = MessageKey.of(ORIGIN, "commands.inspect.title");
    private static final MessageKey MSG_INSPECT_TYPE = MessageKey.of(ORIGIN, "commands.inspect.type");
    private static final MessageKey MSG_INSPECT_STATE = MessageKey.of(ORIGIN, "commands.inspect.state");
    private static final MessageKey MSG_INSPECT_FACING = MessageKey.of(ORIGIN, "commands.inspect.facing");
    private static final MessageKey MSG_INSPECT_ANCHOR = MessageKey.of(ORIGIN, "commands.inspect.anchor");
    private static final MessageKey MSG_INSPECT_HIGHLIGHTED = MessageKey.of(ORIGIN, "commands.inspect.highlighted");

    private final MultiBlockEngine plugin;
    private final ServicesCommandRouter services;
    private final SelectionManager exportSelections;
    private final StructureExporter structureExporter;

    public MultiblockCommand(MultiBlockEngine plugin, SelectionManager exportSelections, StructureExporter structureExporter) {
        this.plugin = plugin;
        this.exportSelections = exportSelections;
        this.structureExporter = structureExporter;
        this.services = new ServicesCommandRouter(plugin);
        this.services.registerInternal(new ItemsCommandService(plugin));
        this.services.registerInternal(new UiCommandService());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;

        if (!sender.hasPermission(PERM_USE) && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
            return true;
        }

        if (safeArgs.length == 0 || safeArgs[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String root = safeArgs[0] == null ? "" : safeArgs[0].toLowerCase(Locale.ROOT);

        if (root.equals("services")) {
            if (!sender.hasPermission(PERM_ADMIN_SERVICES) && !sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            return services.handle(sender, label, safeArgs);
        }

        if (root.equals("admin")) {
            if (!sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            return handleAdmin(sender, label, safeArgs);
        }

        if (root.equals("debug")) {
            if (!sender.hasPermission(PERM_DEBUG)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            return handleDebugLayer(sender, label, safeArgs);
        }

        if (root.equals("inspect")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Este comando solo puede usarlo un jugador.", NamedTextColor.RED));
                return true;
            }
            handleInspect(player, safeArgs);
            return true;
        }

        if (root.equals("status") || root.equals("stats")) {
            handleStatus(sender);
            return true;
        }

        if (root.equals("report")) {
            handleReport(sender, safeArgs);
            return true;
        }

        if (root.equals("assemble") || root.equals("form")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Este comando solo puede usarlo un jugador.", NamedTextColor.RED));
                return true;
            }
            handleAssemble(player);
            return true;
        }

        if (root.equals("disassemble") || root.equals("break")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Este comando solo puede usarlo un jugador.", NamedTextColor.RED));
                return true;
            }
            handleDisassemble(player);
            return true;
        }

        if (root.equals("reload")) {
            if (!sender.hasPermission(PERM_ADMIN_RELOAD) && !sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            handleReload(sender);
            return true;
        }

        if (root.equals("export")) {
            if (!sender.hasPermission(PERM_ADMIN_EXPORT) && !sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Este comando solo puede usarlo un jugador.", NamedTextColor.RED));
                return true;
            }
            handleExport(player, label, safeArgs);
            return true;
        }

        sender.sendMessage(Component.text(tr(sender, MSG_UNKNOWN_SUBCOMMAND), NamedTextColor.RED));
        sendHelp(sender, label);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /" + label + " admin <reload|export|services|<servicio>>", NamedTextColor.YELLOW));
            return true;
        }

        String op = args[1] == null ? "" : args[1].toLowerCase(Locale.ROOT);
        if (op.equals("reload")) {
            if (!sender.hasPermission(PERM_ADMIN_RELOAD) && !sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            handleReload(sender);
            return true;
        }

        if (op.equals("export")) {
            if (!sender.hasPermission(PERM_ADMIN_EXPORT) && !sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Este comando solo puede usarlo un jugador.", NamedTextColor.RED));
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
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            services.sendServicesListPublic(sender);
            return true;
        }

        if (!sender.hasPermission(PERM_ADMIN_SERVICES) && !sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Uso: /" + label + " debug <type|list|<servicio>>", NamedTextColor.YELLOW));
            return true;
        }

        String op = args[1] == null ? "" : args[1].toLowerCase(Locale.ROOT);
        if (op.equals("list") || op.equals("services") || op.equals("service")) {
            if (!sender.hasPermission(PERM_DEBUG_SERVICES) && !sender.hasPermission(PERM_DEBUG)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            services.sendServicesListPublic(sender);
            return true;
        }

        if (op.equals("type")) {
            if (!sender.hasPermission(PERM_DEBUG_SESSION) && !sender.hasPermission(PERM_DEBUG)) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Este comando solo puede usarlo un jugador.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
            return true;
        }

        if (args.length >= 3 && "info".equalsIgnoreCase(args[2])) {
            List<String> remaining = args.length <= 3 ? List.of() : java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 3, args.length));
            return services.dispatch(sender, label, op, "info", remaining);
        }

        List<String> remaining = args.length <= 2 ? List.of() : java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 2, args.length));
        return services.dispatch(sender, label, op, "execute", remaining);
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " inspect", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " assemble", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " disassemble", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " status", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " report [jugador]", NamedTextColor.YELLOW));

        if (sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(Component.text("/" + label + " admin reload", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/" + label + " admin export <start|pos1|pos2|mark|save|cancel>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/" + label + " admin services", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/" + label + " admin <servicio> [info] <args>", NamedTextColor.GRAY));
        }
        if (sender.hasPermission(PERM_DEBUG)) {
            sender.sendMessage(Component.text("/" + label + " debug type <typeId> [jugador]", NamedTextColor.DARK_GRAY));
            sender.sendMessage(Component.text("/" + label + " debug <servicio> [info] <args>", NamedTextColor.DARK_GRAY));
            sender.sendMessage(Component.text("/" + label + " debug list", NamedTextColor.DARK_GRAY));
        }
    }

    

    private void handleExport(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /" + label + " export <start|pos1|pos2|mark|save|cancel>", NamedTextColor.YELLOW));
            return;
        }

        String op = args[1] == null ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (op) {
            case "start" -> {
                exportSelections.start(player);
                player.sendMessage(Component.text("Export session iniciada.", NamedTextColor.GREEN));
            }
            case "cancel" -> {
                boolean cancelled = exportSelections.cancel(player);
                player.sendMessage(Component.text(cancelled ? "Export session cancelada." : "No hay sesión activa.", cancelled ? NamedTextColor.YELLOW : NamedTextColor.RED));
            }
            case "pos1" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                Block b = player.getTargetBlockExact(10);
                if (b == null) {
                    player.sendMessage(Component.text("Debes mirar un bloque (rango 10).", NamedTextColor.RED));
                    return;
                }
                s.setPos1(b.getLocation());
                player.sendMessage(Component.text("pos1 = " + b.getX() + "," + b.getY() + "," + b.getZ(), NamedTextColor.GREEN));
            }
            case "pos2" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                Block b = player.getTargetBlockExact(10);
                if (b == null) {
                    player.sendMessage(Component.text("Debes mirar un bloque (rango 10).", NamedTextColor.RED));
                    return;
                }
                s.setPos2(b.getLocation());
                player.sendMessage(Component.text("pos2 = " + b.getX() + "," + b.getY() + "," + b.getZ(), NamedTextColor.GREEN));
            }
            case "mark" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                if (args.length < 3) {
                    player.sendMessage(Component.text("Uso: /" + label + " export mark <controller|input|output|decorative>", NamedTextColor.YELLOW));
                    return;
                }
                String role = args[2];
                s.setPendingRole(role);
                player.sendMessage(Component.text("Ahora haz click derecho en el bloque para marcar: " + role, NamedTextColor.AQUA));
            }
            case "save" -> {
                ExportSession s = ensureSession(player);
                if (s == null) return;
                if (args.length < 3) {
                    player.sendMessage(Component.text("Uso: /" + label + " export save <id>", NamedTextColor.YELLOW));
                    return;
                }
                String id = args[2];
                try {
                    var res = structureExporter.exportToFile(id, s, plugin.getDataFolder().toPath().resolve("exports"));
                    player.sendMessage(Component.text("Export OK: " + res.id() + " (blocks=" + res.blocks() + ")", NamedTextColor.GREEN));
                    if (!res.warnings().isEmpty()) {
                        player.sendMessage(Component.text("Warnings: " + res.warnings().size(), NamedTextColor.YELLOW));
                    }
                } catch (StructureExporter.ExportException e) {
                    player.sendMessage(Component.text("Export error: " + e.getMessage(), NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text("Subcomando inválido. Uso: /" + label + " export <start|pos1|pos2|mark|save|cancel>", NamedTextColor.RED));
        }
    }

    private ExportSession ensureSession(Player player) {
        ExportSession s = exportSelections.session(player);
        if (s == null) {
            player.sendMessage(Component.text("No hay sesión activa. Usa: /mbe export start", NamedTextColor.RED));
        }
        return s;
    }

    private void handleDebug(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text(tr(player, MSG_DEBUG_USAGE), NamedTextColor.RED));
            return;
        }
        
        String id = args[1];
        Optional<MultiblockType> typeOpt = plugin.getManager().getType(id);
        
        if (typeOpt.isEmpty()) {
            player.sendMessage(Component.text(tr(player, MSG_DEBUG_TYPE_NOT_FOUND, "id", id), NamedTextColor.RED));
            return;
        }
        MultiblockType type = typeOpt.get();

        plugin.getManager().getSource(type.id()).ifPresent(src -> {
            player.sendMessage(Component.text("sourceType=" + src.type().name() + " path=" + src.path(), NamedTextColor.GRAY));
        });
        player.sendMessage(Component.text("signature=" + plugin.getManager().signatureOf(type), NamedTextColor.GRAY));
        
        // Target player
        Player targetPlayer = player;
        if (args.length >= 3) {
            targetPlayer = org.bukkit.Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                player.sendMessage(Component.text(tr(player, MSG_DEBUG_PLAYER_NOT_FOUND, "player", args[2]), NamedTextColor.RED));
                return;
            }
        }
        
        // Raytrace for anchor
        Block targetBlock = targetPlayer.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            player.sendMessage(Component.text(tr(player, MSG_DEBUG_MUST_LOOK_AT_BLOCK), NamedTextColor.RED));
            return;
        }
        
        // Start session
        plugin.getDebugManager().startSession(targetPlayer, type, targetBlock.getLocation());
    }

    private void handleReport(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2 && args[1] != null && !args[1].isBlank()) {
            target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text(tr(sender, MSG_REPORT_PLAYER_NOT_FOUND, "player", args[1]), NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text(tr(sender, MSG_REPORT_PLAYER_NOT_FOUND, "player", "<jugador>"), NamedTextColor.RED));
            return;
        }

        if (plugin.getAssemblyCoordinator() == null) {
            sender.sendMessage(Component.text(tr(sender, MSG_REPORT_NONE), NamedTextColor.YELLOW));
            return;
        }

        AssemblyReport report = plugin.getAssemblyCoordinator().lastReport(target.getUniqueId()).orElse(null);
        if (report == null) {
            sender.sendMessage(Component.text(tr(sender, MSG_REPORT_NONE), NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text(tr(sender, MSG_REPORT_TITLE), NamedTextColor.AQUA));
        sender.sendMessage(Component.text(tr(sender, MSG_REPORT_RESULT, "result", report.result().name()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(tr(sender, MSG_REPORT_TRIGGER, "trigger", report.trigger()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(tr(sender, MSG_REPORT_MULTIBLOCK, "multiblock", report.multiblockId()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(tr(sender, MSG_REPORT_REASON, "reason", report.failureReason()), NamedTextColor.GRAY));
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(Component.text(tr(sender, MSG_RELOAD_START), NamedTextColor.YELLOW));
        
        // Reload Config
        plugin.reloadConfig();

        I18nService i18n = plugin.getAddonManager().getCoreService(I18nService.class);
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
        java.util.Map<String, com.darkbladedev.engine.model.MultiblockSource> sources = new java.util.HashMap<>();
        for (MultiblockParser.LoadedType lt : loaded) {
            if (lt == null || lt.type() == null) {
                continue;
            }
            newTypes.add(lt.type());
            sources.put(lt.type().id(), lt.source());
        }

        plugin.getManager().reloadTypesWithSources(newTypes, sources);
        
        // Restart ticking with new config
        plugin.getManager().startTicking(plugin);
        
        sender.sendMessage(Component.text(tr(sender, MSG_RELOAD_DONE_TYPES, "count", newTypes.size()), NamedTextColor.GREEN));
        sender.sendMessage(Component.text(tr(sender, MSG_RELOAD_DONE_RESTART), NamedTextColor.GREEN));
    }
    
    private void handleStatus(CommandSender sender) {
        MultiblockManager manager = plugin.getManager();
        MetricsManager metrics = manager.getMetrics();
        
        sender.sendMessage(Component.text(tr(sender, MSG_STATUS_TITLE), NamedTextColor.BLUE));
        sender.sendMessage(Component.textOfChildren(
                Component.text(tr(sender, MSG_STATUS_LOADED_TYPES), NamedTextColor.GRAY),
                Component.text(String.valueOf(manager.getTypes().size()), NamedTextColor.WHITE)
        ));
        sender.sendMessage(Component.textOfChildren(
                Component.text(tr(sender, MSG_STATUS_TOTAL_CREATED), NamedTextColor.GRAY),
                Component.text(String.valueOf(metrics.getCreatedInstances()), NamedTextColor.WHITE)
        ));
        sender.sendMessage(Component.textOfChildren(
                Component.text(tr(sender, MSG_STATUS_TOTAL_DESTROYED), NamedTextColor.GRAY),
                Component.text(String.valueOf(metrics.getDestroyedInstances()), NamedTextColor.WHITE)
        ));
        sender.sendMessage(Component.textOfChildren(
                Component.text(tr(sender, MSG_STATUS_STRUCTURE_CHECKS), NamedTextColor.GRAY),
                Component.text(String.valueOf(metrics.getStructureChecks()), NamedTextColor.WHITE)
        ));
        sender.sendMessage(Component.textOfChildren(
                Component.text(tr(sender, MSG_STATUS_AVG_TICK), NamedTextColor.GRAY),
                Component.text(String.format("%.4f ms", metrics.getAverageTickTimeMs()), NamedTextColor.WHITE)
        ));
    }

    private void handleAssemble(Player player) {
        if (plugin.getAssemblyCoordinator() == null) {
            player.sendMessage(Component.text("AssemblyCoordinator no disponible.", NamedTextColor.RED));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Debes mirar un bloque.", NamedTextColor.RED));
            return;
        }

        com.darkbladedev.engine.api.assembly.AssemblyContext ctx = new com.darkbladedev.engine.api.assembly.AssemblyContext(
                com.darkbladedev.engine.api.assembly.AssemblyContext.Cause.MANUAL,
                player,
                target,
                null,
                null,
                null,
                player.isSneaking(),
                java.util.Map.of("command", true)
        );

        AssemblyReport report = plugin.getAssemblyCoordinator().tryAssembleAt(target, ctx);
        if (report == null) {
            player.sendMessage(Component.text("No se pudo intentar el ensamblado.", NamedTextColor.RED));
            return;
        }

        if (report.result() == AssemblyReport.Result.SUCCESS) {
            player.sendMessage(Component.text("Ensamblado OK: " + report.multiblockId(), NamedTextColor.GREEN));
        } else {
            String reason = report.failureReason() == null || report.failureReason().isBlank() ? report.result().name() : report.failureReason();
            player.sendMessage(Component.text("Ensamblado falló: " + reason, NamedTextColor.RED));
        }
    }

    private void handleDisassemble(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Debes mirar un bloque.", NamedTextColor.RED));
            return;
        }

        Optional<MultiblockInstance> instanceOpt = plugin.getManager().getInstanceAt(target.getLocation());
        if (instanceOpt.isEmpty()) {
            player.sendMessage(Component.text("No hay ninguna estructura aquí.", NamedTextColor.YELLOW));
            return;
        }

        MultiblockInstance instance = instanceOpt.get();
        com.darkbladedev.engine.api.event.MultiblockBreakEvent mbEvent = new com.darkbladedev.engine.api.event.MultiblockBreakEvent(instance, player);
        org.bukkit.Bukkit.getPluginManager().callEvent(mbEvent);
        if (mbEvent.isCancelled()) {
            player.sendMessage(Component.text("Acción cancelada.", NamedTextColor.RED));
            return;
        }

        for (com.darkbladedev.engine.model.action.Action a : instance.type().onBreakActions()) {
            try {
                if (a != null) {
                    a.execute(instance, player);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Break action failed: " + (a == null ? "<null>" : a.getClass().getSimpleName()), t);
            }
        }
        plugin.getManager().destroyInstance(instance);
        player.sendMessage(Component.text("Estructura desensamblada.", NamedTextColor.GREEN));
    }

    private void handleInspect(Player player, String[] args) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text(tr(player, MSG_INSPECT_MUST_LOOK_AT_BLOCK), NamedTextColor.RED));
            return;
        }

        MultiblockManager manager = plugin.getManager();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(target.getLocation());

        if (instanceOpt.isEmpty()) {
            player.sendMessage(Component.text(tr(player, MSG_INSPECT_NONE), NamedTextColor.YELLOW));
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

        InspectionPipelineService pipeline = plugin.getAddonManager().getCoreService(InspectionPipelineService.class);
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
                p.sendMessage(Component.text(tr(p, MSG_INSPECT_TITLE), NamedTextColor.GREEN));

                InspectionEntry type = data.entries().get("type");
                if (type != null) {
                    p.sendMessage(Component.textOfChildren(
                            Component.text(tr(p, MSG_INSPECT_TYPE), NamedTextColor.GOLD),
                            Component.text(String.valueOf(type.value()), NamedTextColor.WHITE)
                    ));
                }

                InspectionEntry state = data.entries().get("state");
                if (state != null) {
                    p.sendMessage(Component.textOfChildren(
                            Component.text(tr(p, MSG_INSPECT_STATE), NamedTextColor.GOLD),
                            Component.text(String.valueOf(state.value()), NamedTextColor.WHITE)
                    ));
                }

                InspectionEntry facing = data.entries().get("facing");
                if (facing != null) {
                    p.sendMessage(Component.textOfChildren(
                            Component.text(tr(p, MSG_INSPECT_FACING), NamedTextColor.GOLD),
                            Component.text(String.valueOf(facing.value()), NamedTextColor.WHITE)
                    ));
                }

                InspectionEntry anchor = data.entries().get("anchor");
                if (anchor != null) {
                    p.sendMessage(Component.textOfChildren(
                            Component.text(tr(p, MSG_INSPECT_ANCHOR), NamedTextColor.GOLD),
                            Component.text(String.valueOf(anchor.value()), NamedTextColor.WHITE)
                    ));
                }
            };

            pipeline.inspect(player, InteractionSource.COMMAND, requestedLevel, inspectable, renderer);
        } else {
            player.sendMessage(Component.text(tr(player, MSG_INSPECT_TITLE), NamedTextColor.GREEN));
            player.sendMessage(Component.textOfChildren(
                    Component.text(tr(player, MSG_INSPECT_TYPE), NamedTextColor.GOLD),
                    Component.text(instance.type().id(), NamedTextColor.WHITE)
            ));
            player.sendMessage(Component.textOfChildren(
                    Component.text(tr(player, MSG_INSPECT_STATE), NamedTextColor.GOLD),
                    Component.text(String.valueOf(instance.state()), NamedTextColor.WHITE)
            ));
            player.sendMessage(Component.textOfChildren(
                    Component.text(tr(player, MSG_INSPECT_FACING), NamedTextColor.GOLD),
                    Component.text(String.valueOf(instance.facing()), NamedTextColor.WHITE)
            ));
            player.sendMessage(Component.textOfChildren(
                    Component.text(tr(player, MSG_INSPECT_ANCHOR), NamedTextColor.GOLD),
                    Component.text(formatLoc(instance.anchorLocation()), NamedTextColor.WHITE)
            ));
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
        
        player.sendMessage(Component.text(tr(player, MSG_INSPECT_HIGHLIGHTED), NamedTextColor.AQUA));
    }

    private String tr(CommandSender sender, MessageKey key, Object... params) {
        I18nService i18n = plugin.getAddonManager().getCoreService(I18nService.class);
        if (i18n == null) {
            return key == null ? "" : key.fullKey();
        }
        return i18n.tr(sender, key, params);
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
            roots.add("report");
            if (sender instanceof Player) {
                roots.add("assemble");
                roots.add("disassemble");
            }
            if (sender.hasPermission(PERM_ADMIN_EXPORT) || sender.hasPermission(PERM_ADMIN)) {
                roots.add("export");
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

        if (root.equals("export")) {
            if (safeArgs.length == 2) {
                return filter(List.of("start", "pos1", "pos2", "mark", "save", "cancel"), safeArgs[1]);
            }
            if (safeArgs.length == 3 && "mark".equalsIgnoreCase(safeArgs[1])) {
                return filter(List.of("controller", "input", "output", "decorative"), safeArgs[2]);
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
