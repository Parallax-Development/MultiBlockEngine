package dev.darkblade.mbe.core.application.service.wiring;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.wiring.NodeDescriptor;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultNetworkServiceTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void connectAndDisconnectRebuildsGraph() {
        List<Event> events = new ArrayList<>();
        DefaultNetworkService service = new DefaultNetworkService(events::add);

        NetworkType type = new NetworkType("test:energy");
        Block aBlock = world.getBlockAt(10, 64, 10);
        Block bBlock = world.getBlockAt(11, 64, 10);
        NetworkNode a = service.registerNode(type, aBlock, new NodeDescriptor(Set.of(Direction.NORTH)));
        NetworkNode b = service.registerNode(type, bBlock, new NodeDescriptor(Set.of(Direction.SOUTH)));

        assertTrue(service.connect(type, a, b));
        assertEquals(2, service.getGraph(type, a).nodes().size());
        assertEquals(service.getGraph(type, a).id(), service.getGraph(type, b).id());

        service.disconnect(type, a, b);
        assertEquals(1, service.getGraph(type, a).nodes().size());
        assertEquals(1, service.getGraph(type, b).nodes().size());
        assertNotEquals(service.getGraph(type, a).id(), service.getGraph(type, b).id());
    }
}
