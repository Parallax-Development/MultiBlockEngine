package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.core.application.service.tool.ToolModeContextResolver;

import java.util.Map;
import java.util.Optional;

public final class DebugWiringMode implements ToolMode {

    private static final String PERMISSION = "mbe.tool.mode.debug_wiring";
    private final NetworkService networkService;
    private final ToolModeContextResolver contextResolver;

    public DebugWiringMode(NetworkService networkService, ToolModeContextResolver contextResolver) {
        this.networkService = networkService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "debug_wiring";
    }

    @Override
    public String getDisplayName() {
        return "Debug Wiring";
    }

    @Override
    public boolean supports(WrenchContext context) {
        return networkService != null && context != null && context.clickedBlock() != null;
    }

    @Override
    public WrenchResult handle(WrenchContext context) {
        if (!supports(context) || !ToolModeSecurity.hasPermission(context, PERMISSION)) {
            return WrenchResult.pass();
        }
        
        java.util.Collection<NetworkNode> nodes = networkService.findAllNodes(context.clickedBlock());
        if (nodes.isEmpty()) {
            return WrenchResult.pass();
        }
        
        StringBuilder builder = new StringBuilder();
        for (NetworkNode node : nodes) {
            builder.append(node.type().id()).append(": ").append(networkService.getGraph(node.type(), node).id()).append("\n");
        }
        
        return WrenchResult.success("core.wrench.inspect.anchor", Map.of(
                "anchor", builder.toString().trim()
        ));
    }
}
