package dev.darkblade.mbe.core.infrastructure.compat;

import dev.darkblade.mbe.api.compat.DisplayCompatService;
import dev.darkblade.mbe.api.service.MBEService;
import org.bukkit.plugin.PluginManager;

import java.util.Objects;
import java.util.function.Supplier;

public final class BukkitDisplayCompatService implements DisplayCompatService, MBEService {
    private static final String SERVICE_ID = "mbe:compat.display";

    private final PluginManager pluginManager;
    private final Supplier<Object> previewRendererSupplier;

    public BukkitDisplayCompatService(PluginManager pluginManager, Supplier<Object> previewRendererSupplier) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
        this.previewRendererSupplier = Objects.requireNonNull(previewRendererSupplier, "previewRendererSupplier");
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public boolean isProtocolLibAvailable() {
        return pluginManager.getPlugin("ProtocolLib") != null;
    }

    @Override
    public boolean isPreviewRendererAvailable() {
        return previewRendererSupplier.get() != null;
    }
}
