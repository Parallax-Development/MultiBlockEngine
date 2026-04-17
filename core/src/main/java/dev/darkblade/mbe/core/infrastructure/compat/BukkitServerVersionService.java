package dev.darkblade.mbe.core.infrastructure.compat;

import dev.darkblade.mbe.api.compat.ServerVersion;
import dev.darkblade.mbe.api.compat.ServerVersionService;
import dev.darkblade.mbe.api.service.MBEService;
import org.bukkit.Bukkit;

public final class BukkitServerVersionService implements ServerVersionService, MBEService {
    private static final String SERVICE_ID = "mbe:compat.server-version";

    private volatile ServerVersion cached = ServerVersion.UNKNOWN;

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public void onLoad() {
        cached = resolveCurrent();
    }

    @Override
    public ServerVersion current() {
        ServerVersion value = cached;
        if (!value.isUnknown()) {
            return value;
        }
        value = resolveCurrent();
        cached = value;
        return value;
    }

    private static ServerVersion resolveCurrent() {
        try {
            return ServerVersion.parse(Bukkit.getMinecraftVersion());
        } catch (Throwable ignored) {
            try {
                return ServerVersion.parse(Bukkit.getBukkitVersion());
            } catch (Throwable ignoredToo) {
                return ServerVersion.UNKNOWN;
            }
        }
    }
}
