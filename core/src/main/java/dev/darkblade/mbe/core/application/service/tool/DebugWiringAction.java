package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.core.application.service.tool.mode.ToolModeSecurity;

import java.util.Map;

public final class DebugWiringAction implements ToolAction {

    private static final String PERMISSION = "mbe.tool.mode.debug_wiring";
    private final NetworkService networkService;
    private final ToolModeContextResolver contextResolver;

    public DebugWiringAction(NetworkService networkService, ToolModeContextResolver contextResolver) {
        this.networkService = networkService;
        this.contextResolver = contextResolver;
    }

    @Override
    public ActionId id() {
        return WireCutterActions.DEBUG;
    }

    public boolean supports(WrenchContext context) {
        return networkService != null && context != null && context.clickedBlock() != null;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
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
