package dev.darkblade.mbe.api.ui.runtime;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.ui.runtime.PanelDefinition;
import dev.darkblade.mbe.api.ui.runtime.PanelId;

import java.util.Map;
import java.util.Optional;

public interface UIRuntimeRegistry extends MBEService {
    void registerPanel(PanelId id, PanelDefinition panel);

    Optional<PanelDefinition> getPanel(PanelId id);

    Map<PanelId, PanelDefinition> getAllPanels();

    boolean unregisterPanel(PanelId id);
}
