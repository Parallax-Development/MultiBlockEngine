package dev.darkblade.mbe.core.application.command.structure;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.service.EntryType;
import dev.darkblade.mbe.api.service.Inspectable;
import dev.darkblade.mbe.api.service.InspectionData;
import dev.darkblade.mbe.api.service.InspectionEntry;
import dev.darkblade.mbe.api.service.InspectionLevel;
import dev.darkblade.mbe.api.service.InspectionPipelineService;
import dev.darkblade.mbe.api.service.InspectionRenderer;
import dev.darkblade.mbe.api.service.InteractionSource;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.command.service.impl.AssemblyCommandService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.PatternEntry;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.Default;

import java.util.Map;
import java.util.Optional;

public class StructureCommand {

    private static final String ORIGIN = "mbe";

    private final MultiBlockEngine plugin;
    private final PlayerMessageService messageService;
    private final AssemblyCommandService assemblyCommands;

    public StructureCommand(MultiBlockEngine plugin, PlayerMessageService messageService, AssemblyCommandService assemblyCommands) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.assemblyCommands = assemblyCommands;
    }

    @Command("mbe structure assemble")
    @Permission("multiblockengine.assemble")
    public void assemble(Player player) {
        assemblyCommands.executeAssemble(player);
    }

    @Command("mbe structure disassemble")
    @Permission("multiblockengine.disassemble")
    public void disassemble(Player player) {
        assemblyCommands.executeDisassemble(player);
    }

    @Command("mbe structure inspect [level]")
    @Permission("multiblockengine.inspect")
    public void inspect(
            Player player,
            @Argument("level") @Default("player") String levelStr
    ) {
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
                out.put("type", new InspectionEntry("type", instance.type() == null ? "" : instance.type().id(), EntryType.TEXT, InspectionLevel.PLAYER));
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
            sendMessage(player, MessageKey.of(ORIGIN, "commands.inspect.type"), MessageUtils.params("value", instance.type().id()));
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

    private void sendMessage(Player player, MessageKey key, Map<String, Object> params) {
        messageService.send(player, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
    }
}
