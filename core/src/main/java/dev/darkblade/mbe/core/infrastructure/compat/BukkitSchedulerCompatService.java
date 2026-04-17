package dev.darkblade.mbe.core.infrastructure.compat;

import dev.darkblade.mbe.api.compat.SchedulerCompatService;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.core.MultiBlockEngine;

import java.util.Objects;

public final class BukkitSchedulerCompatService implements SchedulerCompatService, MBEService {
    private static final String SERVICE_ID = "mbe:compat.scheduler";

    private final MultiBlockEngine plugin;

    public BukkitSchedulerCompatService(MultiBlockEngine plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public void runSync(Runnable task) {
        if (task == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        if (task == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }
}
