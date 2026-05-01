package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.io.event.IONetworkSplitEvent;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NodeDescriptor;
import dev.darkblade.mbe.core.application.service.tool.mode.ToolModeSecurity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.block.Action;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SplitNetworkAction implements ToolAction {

    private static final String PERMISSION = "mbe.tool.mode.split_network";
    private final NetworkService networkService;
    private final ToolSessionService sessionService;
    private final ToolModeContextResolver contextResolver;

    public SplitNetworkAction(NetworkService networkService, ToolSessionService sessionService, ToolModeContextResolver contextResolver) {
        this.networkService = networkService;
        this.sessionService = sessionService;
        this.contextResolver = contextResolver;
    }

    @Override
    public ActionId id() {
        return WireCutterActions.SPLIT;
    }

    public boolean supports(WrenchContext context) {
        return networkService != null
                && context != null
                && context.clickedBlock() != null
                && context.action() == Action.RIGHT_CLICK_BLOCK;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        if (!supports(context) || !ToolModeSecurity.hasPermission(context, PERMISSION) || context.player() == null) {
            return WrenchResult.pass();
        }
        Optional<NetworkNode> nodeOpt = contextResolver.resolve(context, Map.of()).node();
        if (nodeOpt.isEmpty()) {
            return WrenchResult.fail("core.wrench.not_found");
        }
        NetworkNode current = nodeOpt.get();
        UUID playerId = context.player().getUniqueId();
        Optional<Map<String, Object>> session = sessionService.get(playerId, "split_network");
        if (session.isEmpty()) {
            sessionService.put(playerId, "split_network", Map.of("from", serialize(current.position()), "type", current.type().id()));
            return WrenchResult.success(null);
        }
        String sessionTypeStr = String.valueOf(session.get().get("type"));
        dev.darkblade.mbe.api.wiring.NetworkType sessionType = new dev.darkblade.mbe.api.wiring.NetworkType(sessionTypeStr);
        Optional<NetworkNode> from = session.flatMap(data -> deserialize(String.valueOf(data.get("from"))))
                .flatMap(pos -> nodeFromPosition(sessionType, pos));
        sessionService.clear(playerId, "split_network");
        if (from.isEmpty()) {
            return WrenchResult.fail("core.wrench.not_found");
        }
        UUID original = networkService.getGraph(sessionType, from.get()).id();
        networkService.disconnect(sessionType, from.get(), current);
        UUID a = networkService.getGraph(sessionType, from.get()).id();
        UUID b = networkService.getGraph(sessionType, current).id();
        Set<UUID> resulting = new LinkedHashSet<>();
        if (a != null) {
            resulting.add(a);
        }
        if (b != null) {
            resulting.add(b);
        }
        if (original != null && resulting.size() > 1) {
            Bukkit.getPluginManager().callEvent(new IONetworkSplitEvent(sessionType, original, List.copyOf(resulting)));
        }
        return WrenchResult.success(null);
    }

    private Optional<NetworkNode> nodeFromPosition(dev.darkblade.mbe.api.wiring.NetworkType type, BlockPos position) {
        if (position == null) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(position.worldId());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(networkService.registerNode(
                type,
                world.getBlockAt(position.x(), position.y(), position.z()),
                new NodeDescriptor(Set.of(dev.darkblade.mbe.api.wiring.Direction.values()))
        ));
    }

    private static String serialize(BlockPos pos) {
        return pos.worldId() + ":" + pos.x() + ":" + pos.y() + ":" + pos.z();
    }

    private static Optional<BlockPos> deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] split = raw.split(":");
        if (split.length != 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BlockPos(
                    UUID.fromString(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]),
                    Integer.parseInt(split[3])
            ));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }
}
