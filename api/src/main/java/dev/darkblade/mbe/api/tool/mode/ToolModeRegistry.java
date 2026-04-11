package dev.darkblade.mbe.api.tool.mode;

import dev.darkblade.mbe.api.service.MBEService;

import java.util.Collection;
import java.util.Optional;

public interface ToolModeRegistry extends MBEService {

    Optional<ToolMode> get(String id);

    Collection<ToolMode> getAll();

    void register(ToolMode mode);
}
