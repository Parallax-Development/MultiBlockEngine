package dev.darkblade.mbe.core.application.service.addon.domain;
import org.bukkit.plugin.ServicePriority;
public record PendingExposure(Class<?> api, Object implementation, ServicePriority priority) {}
