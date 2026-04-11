package dev.darkblade.mbe.core.application.service.addon.crossref;

import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDeclaration;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceHandle;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonCrossReferenceServiceTest {

    @Test
    void compilesBidirectionalLazyReferencesWithoutInvalidCycle() {
        AddonCrossReferenceService manager = new AddonCrossReferenceService();

        manager.declare("addon_a", CrossReferenceDeclaration.builder("addon_a:alpha", Alpha.class, resolver -> new Alpha(resolver.handle("addon_b:beta", Beta.class)))
            .dependsOnRequiredLazy("addon_b:beta")
            .build());

        manager.declare("addon_b", CrossReferenceDeclaration.builder("addon_b:beta", Beta.class, resolver -> new Beta(resolver.handle("addon_a:alpha", Alpha.class)))
            .dependsOnRequiredLazy("addon_a:alpha")
            .build());

        AddonCrossReferenceService.CompilationReport report = manager.compileAndInitialize();
        assertTrue(report.successful());
        assertTrue(manager.resolve("addon_a:alpha", Alpha.class).isPresent());
        assertTrue(manager.resolve("addon_b:beta", Beta.class).isPresent());

        Alpha alpha = manager.resolve("addon_a:alpha", Alpha.class).orElseThrow();
        Beta beta = manager.resolve("addon_b:beta", Beta.class).orElseThrow();
        assertTrue(alpha.beta().isAvailable());
        assertTrue(beta.alpha().isAvailable());
    }

    @Test
    void failsOnInvalidEagerCircularReferences() {
        AddonCrossReferenceService manager = new AddonCrossReferenceService();

        manager.declare("addon_a", CrossReferenceDeclaration.builder("addon_a:alpha", Alpha.class, resolver -> new Alpha(resolver.handle("addon_b:beta", Beta.class)))
            .dependsOnRequiredEager("addon_b:beta")
            .build());

        manager.declare("addon_b", CrossReferenceDeclaration.builder("addon_b:beta", Beta.class, resolver -> new Beta(resolver.handle("addon_a:alpha", Alpha.class)))
            .dependsOnRequiredEager("addon_a:alpha")
            .build());

        AddonCrossReferenceService.CompilationReport report = manager.compileAndInitialize();
        assertFalse(report.successful());
        assertFalse(report.failuresFor("addon_a").isEmpty());
        assertFalse(report.failuresFor("addon_b").isEmpty());
        assertTrue(manager.resolve("addon_a:alpha", Alpha.class).isEmpty());
        assertTrue(manager.resolve("addon_b:beta", Beta.class).isEmpty());
    }

    @Test
    void providesPerformanceMetricsWithoutSignificantOverhead() {
        AddonCrossReferenceService manager = new AddonCrossReferenceService();
        for (int i = 0; i < 1000; i++) {
            String id = "addon_perf:node_" + i;
            manager.declare("addon_perf", CrossReferenceDeclaration.builder(id, String.class, resolver -> id).build());
        }

        AddonCrossReferenceService.CompilationReport report = manager.compileAndInitialize();
        assertTrue(report.successful());
        assertEquals(1000, report.metrics().declaredReferences());
        assertEquals(1000, report.metrics().initializedReferences());
        assertTrue(report.metrics().totalNanos() < 2_000_000_000L);
    }

    private record Alpha(CrossReferenceHandle<Beta> beta) {
    }

    private record Beta(CrossReferenceHandle<Alpha> alpha) {
    }
}
