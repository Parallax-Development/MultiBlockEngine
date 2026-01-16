package com.darkbladedev.engine.manager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsManager {
    
    private final AtomicInteger structureChecks = new AtomicInteger(0);
    private final AtomicInteger createdInstances = new AtomicInteger(0);
    private final AtomicInteger destroyedInstances = new AtomicInteger(0);
    private final AtomicLong totalTickTime = new AtomicLong(0);
    private final AtomicInteger tickCount = new AtomicInteger(0);
    
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
    }
}
