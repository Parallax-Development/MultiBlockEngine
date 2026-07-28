package dev.darkblade.mbe.core.infrastructure.integration;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.api.util.NamespacedKey;
import dev.darkblade.mbe.api.wiring.PortBlockRef;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.api.wiring.PortDirection;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.core.application.service.port.DefaultPortResolutionService;
import dev.darkblade.mbe.core.application.service.wiring.DefaultNetworkService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockWiringBridgeTest {

    private ServerMock server;
    private WorldMock world;
    private DefaultNetworkService networkService;
    private PortResolutionService portResolutionService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        networkService = new DefaultNetworkService(event -> {});
        portResolutionService = new DefaultPortResolutionService();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onMultiblockFormAndBreakRegistersAndUnregistersNodes() {
        MultiblockWiringBridge bridge = new MultiblockWiringBridge(null, networkService, portResolutionService);

        Location anchor = world.getBlockAt(10, 64, 10).getLocation();
        PortDefinition portDef = new PortDefinition("energy_in", PortDirection.INPUT, "ENERGY", new PortBlockRef.Controller(), Set.of("energy"));
        MultiblockType type = new MultiblockType(
                new NamespacedKey("test", "generator"),
                "1.0",
                "wrench",
                new Vector(0, 0, 0),
                null,
                List.of(),
                false,
                Map.of(),
                Map.of(),
                Map.of("energy_in", portDef),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                20
        );
        MultiblockInstance instance = new MultiblockInstance(type, anchor, BlockFace.NORTH);

        MultiblockFormEvent formEvent = new MultiblockFormEvent(instance, null);
        bridge.onMultiblockForm(formEvent);

        assertFalse(bridge.getRegisteredNodes().isEmpty());

        MultiblockBreakEvent breakEvent = new MultiblockBreakEvent(instance, null);
        bridge.onMultiblockBreak(breakEvent);

        assertTrue(bridge.getRegisteredNodes().isEmpty());
    }
}
