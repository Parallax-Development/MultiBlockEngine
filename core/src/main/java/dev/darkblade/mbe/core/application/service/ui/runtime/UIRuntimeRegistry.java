package dev.darkblade.mbe.core.application.service.ui.runtime;

import dev.darkblade.mbe.api.ui.runtime.PanelDefinition;
import dev.darkblade.mbe.api.ui.runtime.PanelId;

import java.util.Map;
import java.util.Optional;

public interface UIRuntimeRegistry {
    void registerPanel(PanelId id, PanelDefinition panel);

    Optional<PanelDefinition> getPanel(PanelId id);

    Map<PanelId, PanelDefinition> getAllPanels();

    boolean unregisterPanel(PanelId id);
}
