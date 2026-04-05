package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.metadata.MetadataServiceImpl;
import dev.darkblade.mbe.core.application.service.metadata.PlayerMultiblockContextResolver;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryServiceImpl;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;
import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiblockExpansionTest {

    @Test
    void listPlaceholderIsSortedAndTruncated() {
        UUID playerId = UUID.randomUUID();
        List<MultiblockInstance> instances = new ArrayList<>();
        for (int i = 60; i >= 1; i--) {
            instances.add(instance(
                    "core:furnace",
                    100 + i,
                    Map.of("owner_uuid", playerId.toString(), "energy", i)
            ));
        }

        TestRuntimeService runtime = new TestRuntimeService(instances);
        PlayerMultiblockQueryServiceImpl queryService = new PlayerMultiblockQueryServiceImpl(runtime, 1_000L);
        MultiblockExpansion expansion = new MultiblockExpansion(
                List.of("test"),
                "1.0.0",
                () -> runtime,
                queryService,
                new MetadataServiceImpl(1_000L),
                new PlayerMultiblockContextResolver(runtime, 12D),
                50
        );

        String value = expansion.onRequest(offlinePlayer(playerId), "player_core:furnace_var_energy_list");

        assertEquals("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50", value);
    }

    @Test
    void percentWithoutMaxVariableReturnsNa() {
        UUID playerId = UUID.randomUUID();
        List<MultiblockInstance> instances = List.of(
                instance("core:furnace", 1, Map.of("owner_uuid", playerId.toString(), "energy", 50)),
                instance("core:furnace", 2, Map.of("owner_uuid", playerId.toString(), "energy", 25))
        );

        TestRuntimeService runtime = new TestRuntimeService(instances);
        PlayerMultiblockQueryServiceImpl queryService = new PlayerMultiblockQueryServiceImpl(runtime, 1_000L);
        MultiblockExpansion expansion = new MultiblockExpansion(
                List.of("test"),
                "1.0.0",
                () -> runtime,
                queryService,
                new MetadataServiceImpl(1_000L),
                new PlayerMultiblockContextResolver(runtime, 12D),
                50
        );

        String value = expansion.onRequest(offlinePlayer(playerId), "player_core:furnace_var_energy_percent");
        assertEquals("N/A", value);
    }

    @Test
    void playerWithoutInstancesReturnsZeroAmount() {
        UUID playerId = UUID.randomUUID();
        TestRuntimeService runtime = new TestRuntimeService(List.of());
        PlayerMultiblockQueryServiceImpl queryService = new PlayerMultiblockQueryServiceImpl(runtime, 1_000L);
        MultiblockExpansion expansion = new MultiblockExpansion(
                List.of("test"),
                "1.0.0",
                () -> runtime,
                queryService,
                new MetadataServiceImpl(1_000L),
                new PlayerMultiblockContextResolver(runtime, 12D),
                50
        );

        String value = expansion.onRequest(offlinePlayer(playerId), "player_core:furnace_amount");
        assertEquals("0", value);
    }

    private static OfflinePlayer offlinePlayer(UUID playerId) {
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class<?>[] {OfflinePlayer.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return playerId;
                    }
                    if ("getName".equals(method.getName())) {
                        return "test";
                    }
                    if ("isOnline".equals(method.getName())) {
                        return false;
                    }
                    if ("getPlayer".equals(method.getName())) {
                        return null;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return playerId.hashCode();
                    }
                    return null;
                }
        );
    }

    private static MultiblockInstance instance(String id, int x, Map<String, Object> vars) {
        return new MultiblockInstance(
                dummyType(id),
                new Location(null, x, 64, 10),
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

    private static final class TestRuntimeService extends MultiblockRuntimeService {
        private final Collection<MultiblockInstance> instances;

        private TestRuntimeService(Collection<MultiblockInstance> instances) {
            this.instances = instances;
        }

        @Override
        public Collection<MultiblockInstance> getActiveInstancesSnapshot() {
            return instances;
        }

        @Override
        public Optional<MultiblockInstance> getInstanceAt(Location loc) {
            return Optional.empty();
        }
    }
}
