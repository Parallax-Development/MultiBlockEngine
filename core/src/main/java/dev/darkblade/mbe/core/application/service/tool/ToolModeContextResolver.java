package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NodeDescriptor;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.tool.mode.IOPortToolSupport;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ToolModeContextResolver {

    private final MultiblockRuntimeService runtimeService;
    private final IOService ioService;
    private final NetworkService networkService;

    public ToolModeContextResolver(MultiblockRuntimeService runtimeService, IOService ioService, NetworkService networkService) {
        this.runtimeService = runtimeService;
        this.ioService = ioService;
        this.networkService = networkService;
    }

    public ToolOperationContext resolve(WrenchContext wrenchContext, Map<String, Object> metadata) {
        Optional<MultiblockInstance> instance = Optional.empty();
        Optional<IOPort> port = Optional.empty();
        Optional<NetworkNode> node = Optional.empty();
        if (wrenchContext != null && wrenchContext.clickedBlock() != null) {
            if (runtimeService != null) {
                instance = runtimeService.getInstanceAt(wrenchContext.clickedBlock().getLocation());
            }
            if (ioService != null && runtimeService != null) {
                port = IOPortToolSupport.findClickedPort(ioService, runtimeService, wrenchContext.clickedBlock());
            }
            if (networkService != null) {
                dev.darkblade.mbe.api.wiring.NetworkType type = null;
                if (metadata != null && metadata.containsKey("networkType") && metadata.get("networkType") instanceof dev.darkblade.mbe.api.wiring.NetworkType nt) {
                    type = nt;
                } else if (port.isPresent()) {
                    type = new dev.darkblade.mbe.api.wiring.NetworkType(port.get().getChannel().name());
                } else {
                    type = new dev.darkblade.mbe.api.wiring.NetworkType("DEFAULT");
                }
                
                node = networkService.findNode(
                        type,
                        wrenchContext.clickedBlock()
                );
                
                if (node.isEmpty()) {
                    node = Optional.ofNullable(networkService.registerNode(
                        type,
                        wrenchContext.clickedBlock(),
                        new NodeDescriptor(Set.of(Direction.values()))
                    ));
                }
            }
        }
        return new ToolOperationContext(
                wrenchContext,
                instance,
                port,
                node,
                metadata == null ? Map.of() : Map.copyOf(metadata)
        );
    }
}
