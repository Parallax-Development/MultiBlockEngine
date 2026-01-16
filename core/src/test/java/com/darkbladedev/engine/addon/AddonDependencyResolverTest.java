package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.addon.Version;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AddonDependencyResolverTest {

    @Test
    void resolve_ordersRequiredDependencies() {
        AddonDependencyResolver resolver = new AddonDependencyResolver();

        AddonMetadata b = new AddonMetadata(
            "b",
            Version.parse("1.0.0"),
            1,
            "com.example.B",
            Map.of(),
            Map.of(),
            List.of()
        );

        AddonMetadata a = new AddonMetadata(
            "a",
            Version.parse("1.0.0"),
            1,
            "com.example.A",
            Map.of("b", Version.parse("1.0.0")),
            Map.of(),
            List.of("b")
        );

        AddonDependencyResolver.Resolution res = resolver.resolve(1, Map.of(
            "a", a,
            "b", b
        ));

        assertEquals(List.of("b", "a"), res.loadOrder());
        assertTrue(res.failures().isEmpty());
    }

    @Test
    void resolve_reportsCycles() {
        AddonDependencyResolver resolver = new AddonDependencyResolver();

        AddonMetadata a = new AddonMetadata(
            "a",
            Version.parse("1.0.0"),
            1,
            "com.example.A",
            Map.of("b", Version.parse("1.0.0")),
            Map.of(),
            List.of("b")
        );

        AddonMetadata b = new AddonMetadata(
            "b",
            Version.parse("1.0.0"),
            1,
            "com.example.B",
            Map.of("a", Version.parse("1.0.0")),
            Map.of(),
            List.of("a")
        );

        AddonDependencyResolver.Resolution res = resolver.resolve(1, Map.of(
            "a", a,
            "b", b
        ));

        assertTrue(res.loadOrder().isEmpty());
        assertEquals("Dependency cycle detected", res.failures().get("a"));
        assertEquals("Dependency cycle detected", res.failures().get("b"));
    }

    @Test
    void resolve_appliesOptionalOrderingWhenPossible() {
        AddonDependencyResolver resolver = new AddonDependencyResolver();

        AddonMetadata b = new AddonMetadata(
            "b",
            Version.parse("1.0.0"),
            1,
            "com.example.B",
            Map.of(),
            Map.of(),
            List.of()
        );

        AddonMetadata a = new AddonMetadata(
            "a",
            Version.parse("1.0.0"),
            1,
            "com.example.A",
            Map.of(),
            Map.of("b", Version.parse("1.0.0")),
            List.of("b")
        );

        AddonDependencyResolver.Resolution res = resolver.resolve(1, Map.of(
            "a", a,
            "b", b
        ));

        assertEquals(List.of("b", "a"), res.loadOrder());
        assertTrue(res.failures().isEmpty());
    }
}
