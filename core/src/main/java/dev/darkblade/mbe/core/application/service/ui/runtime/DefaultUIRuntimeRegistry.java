package dev.darkblade.mbe.core.application.service.ui.runtime;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.ui.runtime.PanelDefinition;
import dev.darkblade.mbe.api.ui.runtime.PanelId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultUIRuntimeRegistry implements UIRuntimeRegistry, MBEService {
    private static final String SERVICE_ID = "mbe:ui.runtime.registry";

    private final Map<PanelId, PanelDefinition> panels = new ConcurrentHashMap<>();

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public void registerPanel(PanelId id, PanelDefinition panel) {
        PanelId panelId = Objects.requireNonNull(id, "id");
        if (panel == null) {
            throw new IllegalArgumentException("panel");
        }
        if (panels.putIfAbsent(panelId, panel) != null) {
            throw new IllegalStateException("Panel already registered: " + panelId.value());
        }
    }

    @Override
    public Optional<PanelDefinition> getPanel(PanelId id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(panels.get(id));
    }

    @Override
    public Map<PanelId, PanelDefinition> getAllPanels() {
        return Map.copyOf(panels);
    }

    @Override
    public boolean unregisterPanel(PanelId id) {
        if (id == null) {
            return false;
        }
        return panels.remove(id) != null;
    }
}
