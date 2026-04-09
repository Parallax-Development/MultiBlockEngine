package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.io.IOType;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.api.wiring.PortDirection;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.core.application.service.io.SimpleIOPort;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public final class IOPortLifecycleListener implements Listener {

    private final IOService ioService;
    private final PortResolutionService portResolutionService;

    public IOPortLifecycleListener(IOService ioService, PortResolutionService portResolutionService) {
        this.ioService = ioService;
        this.portResolutionService = portResolutionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMultiblockForm(MultiblockFormEvent event) {
        MultiblockInstance instance = event.getMultiblock();
        if (instance == null || instance.type() == null || instance.type().ports().isEmpty()) {
            return;
        }
        UUID defaultNetwork = networkFor(instance);
        BlockFace facing = instance.facing() == null ? BlockFace.NORTH : instance.facing();
        Direction face = Direction.fromBlockFace(facing);
        for (PortResolutionService.ResolvedPort resolved : portResolutionService.resolveAll(instance)) {
            PortDefinition definition = resolved.definition();
            Location location = resolved.location();
            if (definition == null || location == null || location.getWorld() == null || face == null) {
                continue;
            }
            IOType type = definition.direction() == PortDirection.INPUT ? IOType.INPUT : IOType.OUTPUT;
            ChannelType channel = channelFrom(definition.type());
            BlockPos blockPos = new BlockPos(
                    location.getWorld().getUID(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
            ioService.registerPort(new SimpleIOPort(
                    blockPos,
                    face,
                    type,
                    channel,
                    defaultNetwork,
                    instance
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMultiblockBreak(MultiblockBreakEvent event) {
        MultiblockInstance instance = event.getMultiblock();
        if (instance == null) {
            return;
        }
        Collection<IOPort> ports = ioService.getPorts(instance);
        if (ports.isEmpty()) {
            return;
        }
        for (IOPort port : ports) {
            ioService.unregisterPort(port);
        }
    }

    private static ChannelType channelFrom(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChannelType.DATA;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return ChannelType.valueOf(normalized);
        } catch (Throwable ignored) {
            return ChannelType.DATA;
        }
    }

    private static UUID networkFor(MultiblockInstance instance) {
        Location anchor = instance.anchorLocation();
        if (anchor == null || anchor.getWorld() == null) {
            return UUID.randomUUID();
        }
        String key = instance.type().id()
                + "|"
                + anchor.getWorld().getUID()
                + "|"
                + anchor.getBlockX()
                + "|"
                + anchor.getBlockY()
                + "|"
                + anchor.getBlockZ();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
