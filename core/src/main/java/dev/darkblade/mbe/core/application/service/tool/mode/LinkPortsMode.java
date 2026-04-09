package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.core.application.service.tool.ToolModeContextResolver;
import dev.darkblade.mbe.core.application.service.tool.ToolSessionService;
import org.bukkit.event.block.Action;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LinkPortsMode implements ToolMode {

    private final IOService ioService;
    private final ToolModeContextResolver contextResolver;
    private final ToolSessionService sessionService;
    private static final String PERMISSION = "mbe.tool.mode.link_ports";

    public LinkPortsMode(IOService ioService, ToolModeContextResolver contextResolver, ToolSessionService sessionService) {
        this.ioService = ioService;
        this.contextResolver = contextResolver;
        this.sessionService = sessionService;
    }

    @Override
    public String getId() {
        return "link_ports";
    }

    @Override
    public String getDisplayName() {
        return "Link Ports";
    }

    @Override
    public boolean supports(WrenchContext context) {
        return context != null && context.action() == Action.RIGHT_CLICK_BLOCK;
    }

    @Override
    public WrenchResult handle(WrenchContext context) {
        Optional<IOPort> portOpt = contextResolver.resolve(context, Map.of()).port();
        if (portOpt.isEmpty()) {
            return WrenchResult.pass();
        }
        if (!ToolModeSecurity.hasPermission(context, PERMISSION) || context.player() == null) {
            return WrenchResult.fail("core.wrench.interaction.denied");
        }
        IOPort current = portOpt.get();
        UUID playerId = context.player().getUniqueId();
        Optional<Map<String, Object>> session = sessionService.get(playerId, getId());
        if (session.isEmpty()) {
            sessionService.put(playerId, getId(), Map.of("origin", serialize(current.getPosition())));
            return WrenchResult.success(null);
        }
        String rawOrigin = String.valueOf(session.get().get("origin"));
        Optional<IOPort> originOpt = deserialize(rawOrigin).flatMap(pos -> ioService instanceof dev.darkblade.mbe.core.application.service.io.DefaultIOService defaultIOService
                ? defaultIOService.findPort(pos)
                : Optional.empty());
        if (originOpt.isEmpty()) {
            sessionService.clear(playerId, getId());
            return WrenchResult.fail("core.wrench.not_found");
        }
        IOPort origin = originOpt.get();
        if (origin.getChannel() != current.getChannel()) {
            return WrenchResult.fail("core.wrench.interaction.denied");
        }
        UUID targetNetwork = origin.getNetworkId();
        IOPortToolSupport.replace(ioService, current, current.getType(), current.getChannel(), targetNetwork);
        sessionService.clear(playerId, getId());
        return WrenchResult.success(null);
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
