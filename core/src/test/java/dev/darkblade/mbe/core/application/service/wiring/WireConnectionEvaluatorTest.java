package dev.darkblade.mbe.core.application.service.wiring;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.wiring.NodeDescriptor;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockInstanceRegistry;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireConnectionEvaluatorTest {

    private ServerMock server;
    private WorldMock world;
    private DefaultNetworkService networkService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        networkService = new DefaultNetworkService(event -> {});
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void canConnectReturnsTrueForValidNodeConnection() {
        WireConnectionEvaluator evaluator = new WireConnectionEvaluator(networkService, null, null);
        NetworkType type = new NetworkType("energy");

        Block sourceBlock = world.getBlockAt(0, 64, 0);
        Block targetBlock = world.getBlockAt(1, 64, 0);

        NetworkNode source = networkService.registerNode(type, sourceBlock, new NodeDescriptor(Set.of(Direction.EAST)));
        networkService.registerNode(type, targetBlock, new NodeDescriptor(Set.of(Direction.WEST)));

        assertTrue(evaluator.canConnect(source, Direction.EAST, targetBlock));
    }

    @Test
    void canConnectReturnsFalseForInvalidSourceFace() {
        WireConnectionEvaluator evaluator = new WireConnectionEvaluator(networkService, null, null);
        NetworkType type = new NetworkType("energy");

        Block sourceBlock = world.getBlockAt(0, 64, 0);
        Block targetBlock = world.getBlockAt(1, 64, 0);

        NetworkNode source = networkService.registerNode(type, sourceBlock, new NodeDescriptor(Set.of(Direction.NORTH)));
        networkService.registerNode(type, targetBlock, new NodeDescriptor(Set.of(Direction.WEST)));

        assertFalse(evaluator.canConnect(source, Direction.EAST, targetBlock));
    }

    @Test
    void canConnectReturnsFalseForInvalidTargetFace() {
        WireConnectionEvaluator evaluator = new WireConnectionEvaluator(networkService, null, null);
        NetworkType type = new NetworkType("energy");

        Block sourceBlock = world.getBlockAt(0, 64, 0);
        Block targetBlock = world.getBlockAt(1, 64, 0);

        NetworkNode source = networkService.registerNode(type, sourceBlock, new NodeDescriptor(Set.of(Direction.EAST)));
        networkService.registerNode(type, targetBlock, new NodeDescriptor(Set.of(Direction.NORTH)));

        assertFalse(evaluator.canConnect(source, Direction.EAST, targetBlock));
    }
}
