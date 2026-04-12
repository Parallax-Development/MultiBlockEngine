package dev.darkblade.mbe.core.application.service.tick;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogBackend;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LoggingConfig;
import dev.darkblade.mbe.api.tick.Tickable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickServiceTest {

    @Test
    void executesTickablesWithDifferentIntervals() {
        TickService service = new TickService(testLogger());
        CountingTickable everyTick = new CountingTickable(1);
        CountingTickable everyFive = new CountingTickable(5);
        CountingTickable everyTwenty = new CountingTickable(20);

        service.register(everyTick);
        service.register(everyFive);
        service.register(everyTwenty);

        for (int i = 0; i < 100; i++) {
            service.executeTickCycle();
        }

        assertEquals(100, everyTick.count.get());
        assertEquals(20, everyFive.count.get());
        assertEquals(5, everyTwenty.count.get());
    }

    @Test
    void isolatesFailuresWithoutBreakingGlobalLoop() {
        TickService service = new TickService(testLogger());
        AtomicInteger faultyExecutions = new AtomicInteger();
        Tickable faulty = new Tickable() {
            @Override
            public void tick() {
                faultyExecutions.incrementAndGet();
                throw new IllegalStateException("boom");
            }
        };
        CountingTickable healthy = new CountingTickable(1);
        service.register(faulty);
        service.register(healthy);

        for (int i = 0; i < 25; i++) {
            service.executeTickCycle();
        }

        assertEquals(25, faultyExecutions.get());
        assertEquals(25, healthy.count.get());
    }

    @Test
    void preventsDuplicateRegistration() {
        TickService service = new TickService(testLogger());
        CountingTickable tickable = new CountingTickable(1);

        assertTrue(service.register(tickable));
        assertFalse(service.register(tickable));

        for (int i = 0; i < 10; i++) {
            service.executeTickCycle();
        }

        assertEquals(10, tickable.count.get());
    }

    @Test
    void defaultsInvalidIntervalsToOne() {
        TickService service = new TickService(testLogger());
        CountingTickable invalid = new CountingTickable(0);
        service.register(invalid);

        for (int i = 0; i < 12; i++) {
            service.executeTickCycle();
        }

        assertEquals(12, invalid.count.get());
    }

    @Test
    void handlesStressWithManyTickables() {
        TickService service = new TickService(testLogger());
        List<CountingTickable> tickables = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            int interval = (i % 20) + 1;
            CountingTickable tickable = new CountingTickable(interval);
            tickables.add(tickable);
            service.register(tickable);
        }

        for (int i = 0; i < 200; i++) {
            service.executeTickCycle();
        }

        int totalExecutions = tickables.stream().mapToInt(t -> t.count.get()).sum();
        int expected = tickables.stream().mapToInt(t -> 200 / t.interval).sum();
        assertEquals(expected, totalExecutions);
    }

    private static CoreLogger testLogger() {
        LogBackend backend = entry -> {
        };
        LoggingConfig config = new LoggingConfig(LogLevel.DEBUG, false, false, false, Set.of());
        return new CoreLogger("TickServiceTest", backend, config);
    }

    private static final class CountingTickable implements Tickable {
        private final int interval;
        private final AtomicInteger count = new AtomicInteger();

        private CountingTickable(int interval) {
            this.interval = interval;
        }

        @Override
        public void tick() {
            count.incrementAndGet();
        }

        @Override
        public int getTickInterval() {
            return interval;
        }
    }
}
