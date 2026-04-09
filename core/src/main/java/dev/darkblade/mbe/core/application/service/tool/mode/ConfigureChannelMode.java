package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.core.application.service.tool.ToolModeContextResolver;
import org.bukkit.event.block.Action;

import java.util.Optional;

public final class ConfigureChannelMode implements ToolMode {

    private final IOService ioService;
    private final ToolModeContextResolver contextResolver;
    private static final String PERMISSION = "mbe.tool.mode.config_channel";

    public ConfigureChannelMode(IOService ioService, ToolModeContextResolver contextResolver) {
        this.ioService = ioService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "config_channel";
    }

    @Override
    public String getDisplayName() {
        return "Channel Config";
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
        ChannelType[] values = ChannelType.values();
        int next = (current.getChannel().ordinal() + 1) % values.length;
        IOPortToolSupport.replace(ioService, current, current.getType(), values[next], current.getNetworkId());
        return WrenchResult.success(null);
    }
}
