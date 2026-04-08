package dev.darkblade.mbe.core.application.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MetricsService {
    
    private final AtomicInteger structureChecks = new AtomicInteger(0);
    private final AtomicInteger createdInstances = new AtomicInteger(0);
    private final AtomicInteger destroyedInstances = new AtomicInteger(0);
    private final AtomicLong totalTickTime = new AtomicLong(0);
    private final AtomicInteger tickCount = new AtomicInteger(0);
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void incrementChecks() {
        if (enabled) structureChecks.incrementAndGet();
    }
    
    public void incrementCreated() {
        if (enabled) createdInstances.incrementAndGet();
    }
    
    public void incrementDestroyed() {
        if (enabled) destroyedInstances.incrementAndGet();
    }
    
    public void recordTickTime(long nanos) {
        if (enabled) {
            totalTickTime.addAndGet(nanos);
            tickCount.incrementAndGet();
        }
    }

    public void increment(String key) {
        if (!enabled || key == null || key.isBlank()) {
            return;
        }
        counters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
    }

    public long counter(String key) {
        if (key == null || key.isBlank()) {
            return 0L;
        }
        AtomicLong counter = counters.get(key);
        return counter == null ? 0L : counter.get();
    }
    
    public double getAverageTickTimeMs() {
        long count = tickCount.get();
        if (count == 0) return 0;
        return (totalTickTime.get() / (double) count) / 1_000_000.0;
    }
    
    public int getStructureChecks() {
        return structureChecks.get();
    }
    
    public int getCreatedInstances() {
        return createdInstances.get();
    }
    
    public int getDestroyedInstances() {
        return destroyedInstances.get();
    }
    
    public void reset() {
        structureChecks.set(0);
        createdInstances.set(0);
        destroyedInstances.set(0);
        totalTickTime.set(0);
        tickCount.set(0);
        counters.clear();
    }
}
