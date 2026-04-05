package dev.darkblade.mbe.core.application.service.query;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.infrastructure.integration.PlaceholderCacheInvalidationListener;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMultiblockQueryServiceImplTest {

    @Test
    void playerWithoutInstancesReturnsZeroAndEmptyCollections() {
        TestRuntimeService runtime = new TestRuntimeService(List.of());
        PlayerMultiblockQueryServiceImpl service = new PlayerMultiblockQueryServiceImpl(runtime, 1_000L);

        UUID playerId = UUID.randomUUID();

        assertEquals(0, service.countInstances(playerId, "core:machine"));
        assertTrue(service.getPlayerInstances(playerId, "core:machine").isEmpty());
        assertTrue(service.getVariableValues(playerId, "core:machine", "energy").isEmpty());
    }

    @Test
    void missingVariableReturnsZeroAggregate() {
        UUID playerId = UUID.randomUUID();
        MultiblockInstance instance = instance("core:furnace", Map.of("owner_uuid", playerId.toString()));
        TestRuntimeService runtime = new TestRuntimeService(List.of(instance));
        PlayerMultiblockQueryServiceImpl service = new PlayerMultiblockQueryServiceImpl(runtime, 1_000L);

        assertEquals(0D, service.aggregate(playerId, "core:furnace", "missing", AggregationType.SUM));
        assertEquals(0D, service.aggregate(playerId, "core:furnace", "missing", AggregationType.AVG));
        assertEquals(0D, service.aggregate(playerId, "core:furnace", "missing", AggregationType.MIN));
        assertEquals(0D, service.aggregate(playerId, "core:furnace", "missing", AggregationType.MAX));
        assertTrue(service.getVariableValues(playerId, "core:furnace", "missing").isEmpty());
    }

    @Test
    void cachePreventsRecalculationUntilInvalidation() {
        UUID playerId = UUID.randomUUID();
        MultiblockInstance instance = instance("core:furnace", Map.of(
                "owner_uuid", playerId.toString(),
                "energy", 40
        ));
        TestRuntimeService runtime = new TestRuntimeService(List.of(instance));
        PlayerMultiblockQueryServiceImpl service = new PlayerMultiblockQueryServiceImpl(runtime, 60_000L);

        double first = service.aggregate(playerId, "core:furnace", "energy", AggregationType.SUM);
        double second = service.aggregate(playerId, "core:furnace", "energy", AggregationType.SUM);

        assertEquals(40D, first);
        assertEquals(40D, second);
        assertEquals(1, runtime.snapshotReads());

        service.invalidatePlayer(playerId);
        double third = service.aggregate(playerId, "core:furnace", "energy", AggregationType.SUM);

        assertEquals(40D, third);
        assertEquals(2, runtime.snapshotReads());
    }

    @Test
    void breakEventInvalidatesCache() {
        UUID playerId = UUID.randomUUID();
        MultiblockInstance instance = instance("core:furnace", Map.of(
                "owner_uuid", playerId.toString(),
                "energy", 10
        ));
        TestRuntimeService runtime = new TestRuntimeService(List.of(instance));
        PlayerMultiblockQueryServiceImpl service = new PlayerMultiblockQueryServiceImpl(runtime, 60_000L);
        PlaceholderCacheInvalidationListener listener = new PlaceholderCacheInvalidationListener(service);

        listener.onMultiblockForm(new MultiblockFormEvent(instance, null));
        service.aggregate(playerId, "core:furnace", "energy", AggregationType.SUM);
        assertEquals(1, runtime.snapshotReads());

        listener.onMultiblockBreak(new MultiblockBreakEvent(instance, null));
        service.aggregate(playerId, "core:furnace", "energy", AggregationType.SUM);
        assertEquals(2, runtime.snapshotReads());
    }

    private static MultiblockInstance instance(String id, Map<String, Object> vars) {
        return new MultiblockInstance(
                dummyType(id),
                new Location(null, 10, 64, 10),
                org.bukkit.block.BlockFace.NORTH,
                dev.darkblade.mbe.core.domain.MultiblockState.ACTIVE,
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

    private static final class TestRuntimeService extends dev.darkblade.mbe.core.application.service.MultiblockRuntimeService {
        private final Collection<MultiblockInstance> instances;
        private int snapshotReads;

        private TestRuntimeService(Collection<MultiblockInstance> instances) {
            this.instances = instances;
        }

        @Override
        public Collection<MultiblockInstance> getActiveInstancesSnapshot() {
            snapshotReads++;
            return instances;
        }

        public int snapshotReads() {
            return snapshotReads;
        }

        @Override
        public Optional<MultiblockInstance> getInstanceAt(Location loc) {
            return Optional.empty();
        }
    }
}
