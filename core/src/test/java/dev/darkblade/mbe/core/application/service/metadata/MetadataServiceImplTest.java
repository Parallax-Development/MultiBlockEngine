package dev.darkblade.mbe.core.application.service.metadata;

import dev.darkblade.mbe.api.metadata.MetadataAccess;
import dev.darkblade.mbe.api.metadata.MetadataKey;
import dev.darkblade.mbe.api.metadata.MetadataKeyBuilder;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;
import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetadataServiceImplTest {

    @Test
    void setAndGetTypedMetadata() {
        MetadataServiceImpl service = new MetadataServiceImpl(1_000L);
        MetadataKey<Integer> energy = MetadataKeyBuilder.<Integer>of("multiblock_energy", Integer.class)
                .apiAccess(MetadataAccess.READ_WRITE)
                .papiAccess(MetadataAccess.READ)
                .formatter(value -> value + " RF")
                .visibility(context -> true)
                .build();
        service.define(energy);
        MultiblockInstance instance = instance("core:furnace", Map.of());

        service.set(instance, energy, 1200);

        assertEquals(1200, service.get(instance, energy));
        assertEquals("1200 RF", service.resolveForPlaceholder("multiblock_energy", new DefaultMetadataContext(instance, null)));
    }

    @Test
    void computedMetadataResolvesWithoutStorage() {
        MetadataServiceImpl service = new MetadataServiceImpl(1_000L);
        service.define(
                MetadataKeyBuilder.<Double>of("multiblock_efficiency", Double.class)
                        .apiAccess(MetadataAccess.READ)
                        .papiAccess(MetadataAccess.READ)
                        .formatter(value -> String.format("%.2f%%", value * 100D))
                        .visibility(context -> true)
                        .computed(context -> {
                            Object energyRaw = context.instance().getVariable("energy");
                            Object maxRaw = context.instance().getVariable("maxEnergy");
                            if (!(energyRaw instanceof Number energyNumber) || !(maxRaw instanceof Number maxNumber)) {
                                return 0D;
                            }
                            double max = maxNumber.doubleValue();
                            if (max <= 0D) {
                                return 0D;
                            }
                            return energyNumber.doubleValue() / max;
                        })
        );

        MultiblockInstance instance = instance("core:furnace", Map.of("energy", 50, "maxEnergy", 100));
        assertEquals(String.format("%.2f%%", 50D), service.resolveForPlaceholder("multiblock_efficiency", new DefaultMetadataContext(instance, null)));
    }

    @Test
    void rejectsInvalidTypeAndUnknownKey() {
        MetadataServiceImpl service = new MetadataServiceImpl(1_000L);
        MetadataKey<Integer> energy = MetadataKeyBuilder.<Integer>of("multiblock_energy", Integer.class)
                .apiAccess(MetadataAccess.READ_WRITE)
                .papiAccess(MetadataAccess.READ)
                .formatter(String::valueOf)
                .visibility(context -> true)
                .build();
        MultiblockInstance instance = instance("core:furnace", Map.of());

        assertThrows(IllegalStateException.class, () -> service.set(instance, energy, 10));

        service.define(energy);
        @SuppressWarnings("rawtypes")
        MetadataKey rawKey = energy;
        assertThrows(IllegalArgumentException.class, () -> service.set(instance, rawKey, "bad"));
    }

    @Test
    void respectsVisibilityAndInvalidatesCacheOnSet() {
        MetadataServiceImpl service = new MetadataServiceImpl(60_000L);
        MetadataKey<Integer> hidden = MetadataKeyBuilder.<Integer>of("multiblock_hidden", Integer.class)
                .apiAccess(MetadataAccess.READ_WRITE)
                .papiAccess(MetadataAccess.READ)
                .formatter(String::valueOf)
                .visibility(context -> false)
                .build();
        MetadataKey<Integer> energy = MetadataKeyBuilder.<Integer>of("multiblock_energy", Integer.class)
                .apiAccess(MetadataAccess.READ_WRITE)
                .papiAccess(MetadataAccess.READ)
                .formatter(String::valueOf)
                .visibility(context -> true)
                .build();
        service.define(hidden);
        service.define(energy);
        MultiblockInstance instance = instance("core:furnace", Map.of());
        service.set(instance, hidden, 5);
        service.set(instance, energy, 10);

        assertNull(service.resolveForPlaceholder("multiblock_hidden", new DefaultMetadataContext(instance, null)));
        assertEquals("10", service.resolveForPlaceholder("multiblock_energy", new DefaultMetadataContext(instance, null)));

        service.set(instance, energy, 25);
        assertEquals("25", service.resolveForPlaceholder("multiblock_energy", new DefaultMetadataContext(instance, null)));
    }

    @Test
    void invalidateInstanceKeyClearsPlaceholderCacheGranularly() {
        MetadataServiceImpl service = new MetadataServiceImpl(60_000L);
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

        service.invalidateInstanceKey(instance, "multiblock_energy");

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
