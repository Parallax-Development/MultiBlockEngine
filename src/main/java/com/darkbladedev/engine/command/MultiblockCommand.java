package com.darkbladedev.engine.command;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.manager.MetricsManager;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.parser.MultiblockParser;
import org.bukkit.ChatColor;
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
import java.util.Optional;

public class MultiblockCommand implements CommandExecutor, TabCompleter {

    private final MultiBlockEngine plugin;

    public MultiblockCommand(MultiBlockEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /mb <inspect|reload>");
            return true;
        }

        if (args[0].equalsIgnoreCase("inspect")) {
            handleInspect(player);
            return true;
        } else if (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("stats")) {
            handleStatus(player);
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            handleReload(player);
            return true;
        } else if (args[0].equalsIgnoreCase("debug")) {
            handleDebug(player, args);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        return true;
    }

    private void handleDebug(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /mb debug <id> [player]");
            return;
        }
        
        String id = args[1];
        Optional<MultiblockType> typeOpt = plugin.getManager().getType(id);
        
        if (typeOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Multiblock type not found: " + id);
            return;
        }
        MultiblockType type = typeOpt.get();
        
        // Target player
        Player targetPlayer = player;
        if (args.length >= 3) {
            targetPlayer = org.bukkit.Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                return;
            }
        }
        
        // Raytrace for anchor
        Block targetBlock = targetPlayer.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must look at a block to use as the controller anchor.");
            return;
        }
        
        // Start session
        plugin.getDebugManager().startSession(targetPlayer, type, targetBlock.getLocation());
    }

    private void handleReload(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Reloading MultiBlockEngine...");
        
        // Reload Config
        plugin.reloadConfig();
        
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
        
        player.sendMessage(ChatColor.GREEN + "Reloaded " + newTypes.size() + " multiblock types.");
        player.sendMessage(ChatColor.GREEN + "Metrics and Ticking restarted.");
    }
    
    private void handleStatus(Player player) {
        MultiblockManager manager = plugin.getManager();
        MetricsManager metrics = manager.getMetrics();
        
        player.sendMessage(ChatColor.BLUE + "=== MultiBlockEngine Status ===");
        player.sendMessage(ChatColor.GRAY + "Loaded Types: " + ChatColor.WHITE + manager.getTypes().size());
        player.sendMessage(ChatColor.GRAY + "Total Created: " + ChatColor.WHITE + metrics.getCreatedInstances());
        player.sendMessage(ChatColor.GRAY + "Total Destroyed: " + ChatColor.WHITE + metrics.getDestroyedInstances());
        player.sendMessage(ChatColor.GRAY + "Structure Checks: " + ChatColor.WHITE + metrics.getStructureChecks());
        player.sendMessage(ChatColor.GRAY + "Avg Tick Time: " + ChatColor.WHITE + String.format("%.4f ms", metrics.getAverageTickTimeMs()));
    }

    private void handleInspect(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "You must look at a block.");
            return;
        }

        MultiblockManager manager = plugin.getManager();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(target.getLocation());

        if (instanceOpt.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No multiblock structure found at this block.");
            return;
        }

        MultiblockInstance instance = instanceOpt.get();
        player.sendMessage(ChatColor.GREEN + "=== Multiblock Info ===");
        player.sendMessage(ChatColor.GOLD + "Type: " + ChatColor.WHITE + instance.type().id());
        player.sendMessage(ChatColor.GOLD + "State: " + ChatColor.WHITE + instance.state());
        player.sendMessage(ChatColor.GOLD + "Facing: " + ChatColor.WHITE + instance.facing());
        player.sendMessage(ChatColor.GOLD + "Anchor: " + ChatColor.WHITE + formatLoc(instance.anchorLocation()));
        
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
        
        player.sendMessage(ChatColor.AQUA + "Structure highlighted.");
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
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("inspect");
            subcommands.add("reload");
            subcommands.add("status");
            subcommands.add("stats");
            subcommands.add("debug");
            
            return filter(subcommands, args[0]);
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
