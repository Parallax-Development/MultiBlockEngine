package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.Map;
import java.util.Optional;

public record ToolOperationContext(
        WrenchContext wrenchContext,
        Optional<MultiblockInstance> instance,
        Optional<IOPort> port,
        Optional<NetworkNode> node,
        Map<String, Object> metadata
) {
}
