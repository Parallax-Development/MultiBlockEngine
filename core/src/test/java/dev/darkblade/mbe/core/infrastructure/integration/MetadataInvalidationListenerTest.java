package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MetadataValueChangeEvent;
import dev.darkblade.mbe.api.metadata.MetadataAccess;
import dev.darkblade.mbe.api.metadata.MetadataKeyBuilder;
import dev.darkblade.mbe.core.application.service.metadata.DefaultMetadataContext;
import dev.darkblade.mbe.core.application.service.metadata.MetadataServiceImpl;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;
import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataInvalidationListenerTest {

    @Test
    void metadataValueChangeEventInvalidatesGranularCache() {
        MetadataServiceImpl service = new MetadataServiceImpl(60_000L);
        MetadataInvalidationListener listener = new MetadataInvalidationListener(service);
        AtomicInteger calls = new AtomicInteger();

        service.define(
                MetadataKeyBuilder.<Integer>of("multiblock_energy", Integer.class)
                        .apiAccess(MetadataAccess.READ)
                        .papiAccess(MetadataAccess.READ)
                        .formatter(String::valueOf)
                        .visibility(context -> true)
                        .computed(context -> calls.incrementAndGet())
        );

        MultiblockInstance instance = instance("core:furnace", Map.of());
        DefaultMetadataContext context = new DefaultMetadataContext(instance, null);

        assertEquals("1", service.resolveForPlaceholder("multiblock_energy", context));
        assertEquals("1", service.resolveForPlaceholder("multiblock_energy", context));

        listener.onMetadataValueChanged(new MetadataValueChangeEvent(instance, "multiblock_energy", 1, 2));

        assertEquals("2", service.resolveForPlaceholder("multiblock_energy", context));
    }

    private static MultiblockInstance instance(String id, Map<String, Object> vars) {
        return new MultiblockInstance(
                dummyType(id),
                new Location(null, 10, 64, 10),
                BlockFace.NORTH,
                MultiblockState.ACTIVE,
                vars
        );
    }

    private static MultiblockType dummyType(String id) {
        return new MultiblockType(
                id,
                "1.0",
                new Vector(0, 0, 0),
                block -> false,
                List.of(),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );
    }
}
