package dev.darkblade.mbe.core.application.service.tick;

import dev.darkblade.mbe.api.logging.EngineLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.tick.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TickService implements MBEService, dev.darkblade.mbe.api.tick.TickService {
    private static final String SERVICE_ID = "mbe:tick.service";
    private static final int DEFAULT_INTERVAL = 1;

    private final Plugin plugin;
    private final EngineLogger logger;
    private final CopyOnWriteArrayList<Tickable> tickables = new CopyOnWriteArrayList<>();
    private final Set<Tickable> warnedInvalidInterval = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicLong currentTick = new AtomicLong(0L);

    private volatile BukkitTask task;

    public TickService(Plugin plugin, EngineLogger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    TickService(EngineLogger logger) {
        this.plugin = null;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public boolean register(Tickable tickable) {
        if (tickable == null) {
            return false;
        }
        if (tickables.contains(tickable)) {
            logger.debug("Tickable already registered",
                LogKv.kv("tickable", tickable.getClass().getName()),
                LogKv.kv("serviceId", SERVICE_ID)
            );
            return false;
        }
        tickables.add(tickable);
        return true;
    }

    @Override
    public boolean unregister(Tickable tickable) {
        if (tickable == null) {
            return false;
        }
        warnedInvalidInterval.remove(tickable);
        return tickables.remove(tickable);
    }

    public void start() {
        if (plugin == null) {
            throw new IllegalStateException("Plugin is required to start TickService");
        }
        if (!started.compareAndSet(false, true)) {
            return;
        }
        currentTick.set(0L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::executeTickCycle, 1L, 1L);
        logger.info("Tick service started", LogKv.kv("serviceId", SERVICE_ID));
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        BukkitTask runningTask = task;
        if (runningTask != null) {
            runningTask.cancel();
        }
        task = null;
        currentTick.set(0L);
        logger.info("Tick service stopped", LogKv.kv("serviceId", SERVICE_ID));
    }

    void executeTickCycle() {
        long tick = currentTick.incrementAndGet();
        for (Tickable tickable : tickables) {
            int interval = resolveInterval(tickable);
            if (tick % interval != 0L) {
                continue;
            }
            try {
                tickable.tick();
            } catch (Throwable throwable) {
                logger.error("Tickable execution failed",
                    throwable,
                    LogKv.kv("tickable", tickable.getClass().getName()),
                    LogKv.kv("interval", interval),
                    LogKv.kv("tick", tick)
                );
            }
        }
    }

    private int resolveInterval(Tickable tickable) {
        int interval;
        try {
            interval = tickable.getTickInterval();
        } catch (Throwable throwable) {
            logger.warn("Tickable interval resolution failed, defaulting to 1",
                LogKv.kv("tickable", tickable.getClass().getName()),
                LogKv.kv("errorType", throwable.getClass().getName())
            );
            return DEFAULT_INTERVAL;
        }
        if (interval > 0) {
            warnedInvalidInterval.remove(tickable);
            return interval;
        }
        if (warnedInvalidInterval.add(tickable)) {
            logger.warn("Tickable interval is invalid, defaulting to 1",
                LogKv.kv("tickable", tickable.getClass().getName()),
                LogKv.kv("interval", interval)
            );
        }
        return DEFAULT_INTERVAL;
    }
}
