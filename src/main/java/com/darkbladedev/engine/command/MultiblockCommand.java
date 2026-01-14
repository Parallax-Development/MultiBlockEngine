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
        if (safeArgs.length > 0 && safeArgs[0].equalsIgnoreCase("services")) {
            return services.handle(sender, label, safeArgs);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(tr(sender, MSG_USAGE_CONSOLE, "label", label), NamedTextColor.YELLOW));
            return true;
        }

        if (safeArgs.length == 0) {
            player.sendMessage(Component.text(tr(player, MSG_USAGE_PLAYER, "label", label), NamedTextColor.YELLOW));
            return true;
        }

        if (safeArgs[0].equalsIgnoreCase("inspect")) {
            handleInspect(player);
            return true;
        } else if (safeArgs[0].equalsIgnoreCase("export")) {
            handleExport(player, label, safeArgs);
            return true;
        } else if (safeArgs[0].equalsIgnoreCase("status") || safeArgs[0].equalsIgnoreCase("stats")) {
            handleStatus(player);
            return true;
        } else if (safeArgs[0].equalsIgnoreCase("reload")) {
            handleReload(player);
            return true;
        } else if (safeArgs[0].equalsIgnoreCase("debug")) {
            handleDebug(player, safeArgs);
            return true;
        } else if (safeArgs[0].equalsIgnoreCase("report")) {
            handleReport(player, safeArgs);
            return true;
        }

        player.sendMessage(Component.text(tr(player, MSG_UNKNOWN_SUBCOMMAND), NamedTextColor.RED));
        return true;
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

    private void handleReport(Player player, String[] args) {
        Player target = player;
        if (args.length >= 2) {
            target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Component.text(tr(player, MSG_REPORT_PLAYER_NOT_FOUND, "player", args[1]), NamedTextColor.RED));
                return;
            }
        }

        if (plugin.getAssemblyCoordinator() == null) {
            player.sendMessage(Component.text(tr(player, MSG_REPORT_NONE), NamedTextColor.YELLOW));
            return;
        }

        AssemblyReport report = plugin.getAssemblyCoordinator()
                .lastReport(target.getUniqueId())
                .orElse(null);

        if (report == null) {
            player.sendMessage(Component.text(tr(player, MSG_REPORT_NONE), NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text(tr(player, MSG_REPORT_TITLE), NamedTextColor.AQUA));
        player.sendMessage(Component.text(tr(player, MSG_REPORT_RESULT, "result", report.result().name()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(tr(player, MSG_REPORT_TRIGGER, "trigger", report.trigger()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(tr(player, MSG_REPORT_MULTIBLOCK, "multiblock", report.multiblockId()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(tr(player, MSG_REPORT_REASON, "reason", report.failureReason()), NamedTextColor.GRAY));
    }

    private void handleReload(Player player) {
        player.sendMessage(Component.text(tr(player, MSG_RELOAD_START), NamedTextColor.YELLOW));
        
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
        List<MultiblockType> newTypes = parser.loadAll(multiblockDir);
        
        plugin.getManager().reloadTypes(newTypes);
        
        // Restart ticking with new config
        plugin.getManager().startTicking(plugin);
        
        player.sendMessage(Component.text(tr(player, MSG_RELOAD_DONE_TYPES, "count", newTypes.size()), NamedTextColor.GREEN));
        player.sendMessage(Component.text(tr(player, MSG_RELOAD_DONE_RESTART), NamedTextColor.GREEN));
    }
    
    private void handleStatus(Player player) {
        MultiblockManager manager = plugin.getManager();
        MetricsManager metrics = manager.getMetrics();
        
        player.sendMessage(Component.text(tr(player, MSG_STATUS_TITLE), NamedTextColor.BLUE));
        player.sendMessage(Component.textOfChildren(
                Component.text(tr(player, MSG_STATUS_LOADED_TYPES), NamedTextColor.GRAY),
                Component.text(String.valueOf(manager.getTypes().size()), NamedTextColor.WHITE)
        ));
        player.sendMessage(Component.textOfChildren(
                Component.text(tr(player, MSG_STATUS_TOTAL_CREATED), NamedTextColor.GRAY),
                Component.text(String.valueOf(metrics.getCreatedInstances()), NamedTextColor.WHITE)
        ));
        player.sendMessage(Component.textOfChildren(
                Component.text(tr(player, MSG_STATUS_TOTAL_DESTROYED), NamedTextColor.GRAY),
                Component.text(String.valueOf(metrics.getDestroyedInstances()), NamedTextColor.WHITE)
        ));
        player.sendMessage(Component.textOfChildren(
                Component.text(tr(player, MSG_STATUS_STRUCTURE_CHECKS), NamedTextColor.GRAY),
                Component.text(String.valueOf(metrics.getStructureChecks()), NamedTextColor.WHITE)
        ));
        player.sendMessage(Component.textOfChildren(
                Component.text(tr(player, MSG_STATUS_AVG_TICK), NamedTextColor.GRAY),
                Component.text(String.format("%.4f ms", metrics.getAverageTickTimeMs()), NamedTextColor.WHITE)
        ));
    }

    private void handleInspect(Player player) {
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
        if (safeArgs.length > 0 && safeArgs[0].equalsIgnoreCase("services")) {
            return services.tabComplete(sender, safeArgs);
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("inspect");
            subcommands.add("export");
            subcommands.add("reload");
            subcommands.add("status");
            subcommands.add("stats");
            subcommands.add("debug");
            subcommands.add("report");
            subcommands.add("services");
            
            return filter(subcommands, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("export")) {
            return filter(List.of("start", "pos1", "pos2", "mark", "save", "cancel"), args[1]);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("export") && args[1].equalsIgnoreCase("mark")) {
            return filter(List.of("controller", "input", "output", "decorative"), args[2]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            // Autocomplete multiblock types
            List<String> types = new ArrayList<>();
            for (MultiblockType type : plugin.getManager().getTypes()) {
                types.add(type.id());
            }
            return filter(types, args[1]);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("debug")) {
            // Autocomplete players
            return null; // Bukkit default player completion
        } else if (args.length == 2 && args[0].equalsIgnoreCase("report")) {
            return null;
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
