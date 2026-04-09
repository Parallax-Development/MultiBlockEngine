package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.core.application.service.tool.ToolModeContextResolver;

import java.util.Optional;

public final class DebugIOMode implements ToolMode {

    private final IOService ioService;
    private final ToolModeContextResolver contextResolver;
    private static final String PERMISSION = "mbe.tool.mode.debug_io";

    public DebugIOMode(IOService ioService, ToolModeContextResolver contextResolver) {
        this.ioService = ioService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "debug_io";
    }

    @Override
    public String getDisplayName() {
        return "Debug IO";
    }

    @Override
    public boolean supports(WrenchContext context) {
        return context != null && context.clickedBlock() != null;
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
        IOPort port = portOpt.get();
        return WrenchResult.success("core.wrench.inspect.type", java.util.Map.of(
                "type", port.getChannel().name() + ":" + port.getType().name()
        ));
    }
}
