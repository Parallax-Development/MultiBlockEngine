package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogBackend;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LoggingConfig;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDeclaration;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceHandle;
import dev.darkblade.mbe.api.addon.crossref.InjectCrossReference;
import dev.darkblade.mbe.api.service.InjectService;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.EnergyProvider;
import dev.darkblade.mbe.core.application.service.addon.crossref.AddonCrossReferenceService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceLifecycleOrchestratorTest {

    @Test
    void registrySupportsLookupByIdAndTypeAndListeners() {
        CoreLogger logger = testLogger();
        MBEServiceRegistry registry = new MBEServiceRegistry();
        ServiceInjector injector = new ServiceInjector(registry, logger);
        ServiceLifecycleOrchestrator lifecycle = new ServiceLifecycleOrchestrator(registry, injector, logger);

        AtomicInteger notifications = new AtomicInteger();
        lifecycle.addListener(service -> notifications.incrementAndGet());

        ProducerService p1 = new ProducerService("mbe:test.energy.one", 120, 500);
        ProducerService p2 = new ProducerService("mbe:test.energy.two", 240, 500);
        lifecycle.registerService("mbe:addon_a", p1);
        lifecycle.registerService("mbe:addon_b", p2);

        assertEquals(2, lifecycle.getByType(EnergyProvider.class).size());
        assertTrue(lifecycle.get("mbe:test.energy.one", EnergyProvider.class).isPresent());
        assertEquals(2, notifications.get());
    }

    @Test
    void injectorSupportsOptionalAndMissingServiceWithoutFailure() {
        CoreLogger logger = testLogger();
        MBEServiceRegistry registry = new MBEServiceRegistry();
        ServiceInjector injector = new ServiceInjector(registry, logger);

        MissingDependencyTarget target = new MissingDependencyTarget();
        injector.inject(target, "mbe:addon_missing");

        assertNull(target.required);
        assertNotNull(target.optional);
        assertTrue(target.optional.isEmpty());
    }

    @Test
    void lifecycleRegistersInjectsAndEnablesDependentServices() {
        CoreLogger logger = testLogger();
        MBEServiceRegistry registry = new MBEServiceRegistry();
        ServiceInjector injector = new ServiceInjector(registry, logger);
        ServiceLifecycleOrchestrator lifecycle = new ServiceLifecycleOrchestrator(registry, injector, logger);

        ProducerService producer = new ProducerService("mbe:test.energy.provider", 300, 1000);
        ConsumerService consumer = new ConsumerService("mbe:test.energy.consumer");

        lifecycle.registerService("mbe:power_addon", producer);
        lifecycle.registerService("mbe:machine_addon", consumer);
        lifecycle.injectServices("mbe:machine_addon");
        lifecycle.enableServices("mbe:machine_addon");

        assertTrue(producer.loaded);
        assertTrue(consumer.enabledWithDependency);

        lifecycle.disableServices("mbe:machine_addon");
        assertTrue(consumer.disabled);
        assertFalse(lifecycle.get("mbe:test.energy.consumer", MBEService.class).isPresent());
    }

    @Test
    void injectorSupportsCrossReferenceInjectionWithoutStrongCoupling() {
        CoreLogger logger = testLogger();
        MBEServiceRegistry registry = new MBEServiceRegistry();
        AddonCrossReferenceService crossReferenceManager = new AddonCrossReferenceService();
        crossReferenceManager.declare("mbe", CrossReferenceDeclaration.builder("mbe:cross.alpha", String.class, resolver -> "alpha").build());
        crossReferenceManager.compileAndInitialize();

        ServiceInjector injector = new ServiceInjector(registry, crossReferenceManager, logger);
        CrossReferenceTarget target = new CrossReferenceTarget();
        injector.inject(target, "mbe:test");

        assertTrue(target.direct != null && target.direct.equals("alpha"));
        assertNotNull(target.handle);
        assertTrue(target.handle.resolve().isPresent());
        assertEquals("alpha", target.handle.resolve().orElseThrow());
    }

    private static CoreLogger testLogger() {
        LogBackend backend = entry -> {
        };
        LoggingConfig cfg = new LoggingConfig(LogLevel.DEBUG, false, false, false, Set.of());
        return new CoreLogger("Test", backend, cfg);
    }

    private static final class MissingDependencyTarget {
        @InjectService("mbe:missing.required")
        private ProducerService required;

        @InjectService("mbe:missing.optional")
        private Optional<ProducerService> optional = Optional.empty();
    }

    private static final class CrossReferenceTarget {
        @InjectCrossReference("mbe:cross.alpha")
        private String direct;

        @InjectCrossReference("mbe:cross.alpha")
        private CrossReferenceHandle<String> handle = CrossReferenceHandle.unresolved();
    }

    private static final class ProducerService implements MBEService, EnergyProvider {
        private final String serviceId;
        private final int energy;
        private final int max;
        private boolean loaded;

        private ProducerService(String serviceId, int energy, int max) {
            this.serviceId = serviceId;
            this.energy = energy;
            this.max = max;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public void onLoad() {
            loaded = true;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return max;
        }
    }

    private static final class ConsumerService implements MBEService {
        private final String serviceId;
        private boolean enabledWithDependency;
        private boolean disabled;

        @InjectService("mbe:test.energy.provider")
        private EnergyProvider provider;

        private ConsumerService(String serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public void onEnable() {
            enabledWithDependency = provider != null && provider.getEnergyStored() == 300;
        }

        @Override
        public void onDisable() {
            disabled = true;
        }
    }
}
