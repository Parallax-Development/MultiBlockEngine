package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.api.ui.runtime.PanelDefinition;
import dev.darkblade.mbe.api.ui.runtime.PanelId;
import dev.darkblade.mbe.api.ui.runtime.PanelOpenService;
import dev.darkblade.mbe.core.application.service.ServiceLifecycleOrchestrator;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import dev.darkblade.mbe.core.application.service.ui.runtime.UIRuntimeRegistry;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PanelViewServiceImpl implements PanelViewService, MBEService {
    private static final String SERVICE_ID = "mbe:ui.panel.view";

    private final UIRuntimeRegistry registry;
    private final AddonLifecycleService addonLifecycleService;

    public PanelViewServiceImpl(UIRuntimeRegistry registry, AddonLifecycleService addonLifecycleService) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.addonLifecycleService = Objects.requireNonNull(addonLifecycleService, "addonLifecycleService");
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public void registerPanel(PanelId id, PanelDefinition panel) {
        if (addonLifecycleService.getCurrentLifecyclePhase() != ServiceLifecycleOrchestrator.LifecyclePhase.CONTENT_REGISTRATION) {
            throw new IllegalStateException("Panel registration outside allowed phase");
        }
        registry.registerPanel(id, panel);
    }

    @Override
    public Optional<PanelDefinition> getPanel(PanelId id) {
        return registry.getPanel(id);
    }

    @Override
    public Map<PanelId, PanelDefinition> getAllPanels() {
        return registry.getAllPanels();
    }

    @Override
    public boolean unregisterPanel(PanelId id) {
        return registry.unregisterPanel(id);
    }

    @Override
    public void openPanel(Player player, String panelId) {
        if (player == null || panelId == null || panelId.isBlank()) {
            return;
        }
        if (!panelExists(panelId)) {
            return;
        }
        PanelOpenService opener = addonLifecycleService.getService(PanelOpenService.class);
        if (opener != null) {
            opener.openPanel(player, panelId);
        }
    }
}
