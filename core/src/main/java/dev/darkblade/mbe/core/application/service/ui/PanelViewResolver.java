package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class PanelViewResolver {
    private PanelViewResolver() {
    }

    public static PanelViewService resolve() {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin != null) {
            AddonLifecycleService lifecycle = plugin.getAddonLifecycleService();
            if (lifecycle != null) {
                java.util.List<PanelViewService> dynamic = lifecycle.getServicesByType(PanelViewService.class);
                if (dynamic != null && !dynamic.isEmpty() && dynamic.get(0) != null) {
                    return dynamic.get(0);
                }
                PanelViewService inProcess = lifecycle.getService(PanelViewService.class);
                if (inProcess != null) {
                    return inProcess;
                }
            }
        }
        RegisteredServiceProvider<PanelViewService> typed = Bukkit.getServicesManager().getRegistration(PanelViewService.class);
        if (typed != null && typed.getProvider() != null) {
            return typed.getProvider();
        }
        return null;
    }
}
