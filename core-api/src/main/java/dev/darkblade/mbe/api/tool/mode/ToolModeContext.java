package dev.darkblade.mbe.api.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.Map;
import java.util.Optional;

public record ToolModeContext(
        WrenchContext wrenchContext,
        MultiblockInstance instance,
        Optional<IOPort> port,
        Map<String, Object> metadata
) {
}
