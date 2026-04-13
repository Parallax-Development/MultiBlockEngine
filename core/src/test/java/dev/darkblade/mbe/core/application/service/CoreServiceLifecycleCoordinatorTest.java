package dev.darkblade.mbe.core.application.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoreServiceLifecycleCoordinatorTest {

    @Test
    void executesLoadAndEnableInRegistrationOrder() {
        CoreServiceLifecycleCoordinator coordinator = new CoreServiceLifecycleCoordinator();
        List<String> events = new ArrayList<>();

        coordinator.register("mbe:a", () -> events.add("load-a"), () -> events.add("enable-a"), null);
        coordinator.register("mbe:b", () -> events.add("load-b"), () -> events.add("enable-b"), null);

        coordinator.loadAll();
        coordinator.enableAll();

        assertEquals(List.of("load-a", "load-b", "enable-a", "enable-b"), events);
    }

    @Test
    void executesDisableInReverseRegistrationOrder() {
        CoreServiceLifecycleCoordinator coordinator = new CoreServiceLifecycleCoordinator();
        List<String> events = new ArrayList<>();

        coordinator.register("mbe:a", null, null, () -> events.add("disable-a"));
        coordinator.register("mbe:b", null, null, () -> events.add("disable-b"));

        coordinator.disableAll();

        assertEquals(List.of("disable-b", "disable-a"), events);
    }

    @Test
    void disableAggregatesFailuresAndContinues() {
        CoreServiceLifecycleCoordinator coordinator = new CoreServiceLifecycleCoordinator();
        List<String> events = new ArrayList<>();

        coordinator.register("mbe:a", null, null, () -> {
            events.add("disable-a");
            throw new IllegalStateException("a");
        });
        coordinator.register("mbe:b", null, null, () -> {
            events.add("disable-b");
            throw new IllegalStateException("b");
        });

        RuntimeException ex = assertThrows(RuntimeException.class, coordinator::disableAll);

        assertEquals(List.of("disable-b", "disable-a"), events);
        assertEquals(2, ex.getSuppressed().length);
    }

    @Test
    void registerManagedCoreServiceUsesContractLifecycleMethods() {
        CoreServiceLifecycleCoordinator coordinator = new CoreServiceLifecycleCoordinator();
        List<String> events = new ArrayList<>();
        ManagedCoreService managedCoreService = new ManagedCoreService() {
            @Override
            public String getManagedCoreServiceId() {
                return "mbe:test-managed";
            }

            @Override
            public void onCoreLoad() {
                events.add("load");
            }

            @Override
            public void onCoreEnable() {
                events.add("enable");
            }

            @Override
            public void onCoreDisable() {
                events.add("disable");
            }
        };

        coordinator.register(managedCoreService);
        coordinator.loadAll();
        coordinator.enableAll();
        coordinator.disableAll();

        assertEquals(List.of("load", "enable", "disable"), events);
    }
}
