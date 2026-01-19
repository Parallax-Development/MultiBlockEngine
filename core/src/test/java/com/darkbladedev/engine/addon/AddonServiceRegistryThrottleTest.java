package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.EngineLogger;
import com.darkbladedev.engine.api.logging.LogBackend;
import com.darkbladedev.engine.api.logging.LogEntry;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LoggingConfig;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddonServiceRegistryThrottleTest {

    @Test
    void successfulResolvesAreThrottled() {
        CapturingBackend backend = new CapturingBackend();
        LoggingConfig config = new LoggingConfig(LogLevel.INFO, false, true, false, Set.of());
        CoreLogger logger = new CoreLogger("MultiBlockEngine", backend, config);

        AtomicLong now = new AtomicLong(0L);
        AddonServiceRegistry registry = new AddonServiceRegistry(logger, Duration.ofMillis(100), now::get);

        EngineLogger svc = (level, message, throwable, fields) -> {
        };
        registry.register("mbe:core", EngineLogger.class, svc);

        registry.resolveIfEnabled("mbe:core", EngineLogger.class, id -> AddonManager.AddonState.ENABLED);
        now.addAndGet(Duration.ofMillis(50).toNanos());
        registry.resolveIfEnabled("mbe:core", EngineLogger.class, id -> AddonManager.AddonState.ENABLED);
        now.addAndGet(Duration.ofMillis(120).toNanos());
        registry.resolveIfEnabled("mbe:core", EngineLogger.class, id -> AddonManager.AddonState.ENABLED);

        assertEquals(2, backend.count(LogLevel.TRACE, "Service resolved"));
    }

    @Test
    void blockedResolvesAreThrottled() {
        CapturingBackend backend = new CapturingBackend();
        LoggingConfig config = new LoggingConfig(LogLevel.INFO, false, true, false, Set.of());
        CoreLogger logger = new CoreLogger("MultiBlockEngine", backend, config);

        AtomicLong now = new AtomicLong(0L);
        AddonServiceRegistry registry = new AddonServiceRegistry(logger, Duration.ofMillis(100), now::get);

        EngineLogger svc = (level, message, throwable, fields) -> {
        };
        registry.register("mbe:core", EngineLogger.class, svc);

        registry.resolveIfEnabled("mbe:core", EngineLogger.class, id -> AddonManager.AddonState.DISABLED);
        now.addAndGet(Duration.ofMillis(50).toNanos());
        registry.resolveIfEnabled("mbe:core", EngineLogger.class, id -> AddonManager.AddonState.DISABLED);
        now.addAndGet(Duration.ofMillis(120).toNanos());
        registry.resolveIfEnabled("mbe:core", EngineLogger.class, id -> AddonManager.AddonState.DISABLED);

        assertEquals(2, backend.count(LogLevel.DEBUG, "Service resolve blocked: provider not enabled"));
    }

    private static final class CapturingBackend implements LogBackend {

        private final List<LogEntry> entries = new ArrayList<>();

        @Override
        public void publish(LogEntry entry) {
            entries.add(entry);
        }

        int count(LogLevel level, String message) {
            int out = 0;
            for (LogEntry e : entries) {
                if (e == null) {
                    continue;
                }
                if (e.level() != level) {
                    continue;
                }
                if (!message.equals(e.message())) {
                    continue;
                }
                out++;
            }
            return out;
        }
    }
}

