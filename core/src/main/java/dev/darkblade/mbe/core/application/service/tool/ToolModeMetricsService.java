package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.service.MBEService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ToolModeMetricsService implements MBEService {

    private final ConcurrentHashMap<String, AtomicLong> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> successes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failures = new ConcurrentHashMap<>();

    @Override
    public String getServiceId() {
        return "mbe:tool.metrics";
    }

    public void recordAttempt(String modeId) {
        counter(attempts, modeId).incrementAndGet();
    }

    public void recordSuccess(String modeId) {
        counter(successes, modeId).incrementAndGet();
    }

    public void recordFailure(String modeId) {
        counter(failures, modeId).incrementAndGet();
    }

    public Map<String, Long> attempts() {
        return snapshot(attempts);
    }

    public Map<String, Long> successes() {
        return snapshot(successes);
    }

    public Map<String, Long> failures() {
        return snapshot(failures);
    }

    private static AtomicLong counter(ConcurrentHashMap<String, AtomicLong> store, String modeId) {
        String key = modeId == null ? "unknown" : modeId.trim().toLowerCase(java.util.Locale.ROOT);
        return store.computeIfAbsent(key, unused -> new AtomicLong());
    }

    private static Map<String, Long> snapshot(ConcurrentHashMap<String, AtomicLong> source) {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : source.entrySet()) {
            out.put(entry.getKey(), entry.getValue().get());
        }
        return Map.copyOf(out);
    }
}
