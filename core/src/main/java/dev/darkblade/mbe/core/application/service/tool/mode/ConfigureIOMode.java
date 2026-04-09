package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOType;
import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.core.application.service.tool.ToolModeContextResolver;
import org.bukkit.event.block.Action;

import java.util.Optional;

public final class ConfigureIOMode implements ToolMode {

    private static final String PERMISSION = "mbe.tool.mode.config_io";
    private final dev.darkblade.mbe.api.io.IOService ioService;
    private final ToolModeContextResolver contextResolver;

    public ConfigureIOMode(dev.darkblade.mbe.api.io.IOService ioService, ToolModeContextResolver contextResolver) {
        this.ioService = ioService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "config_io";
    }

    @Override
    public String getDisplayName() {
        return "I/O Config";
    }

    @Override
    public boolean supports(WrenchContext context) {
        return context != null && context.action() == Action.RIGHT_CLICK_BLOCK;
    }

    @Override
    public WrenchResult handle(WrenchContext context) {
        Optional<IOPort> portOpt = contextResolver.resolve(context, java.util.Map.of()).port();
        if (portOpt.isEmpty()) {
            return WrenchResult.pass();
        }
        if (!ToolModeSecurity.hasPermission(context, PERMISSION)) {
            return WrenchResult.fail("core.wrench.interaction.denied");
        }
        IOPort current = portOpt.get();
        IOType next = switch (current.getType()) {
            case INPUT -> IOType.OUTPUT;
            case OUTPUT -> IOType.BOTH;
            case BOTH -> IOType.INPUT;
        };
        IOPortToolSupport.replace(ioService, current, next, current.getChannel(), current.getNetworkId());
        return WrenchResult.success(null);
    }
}
