package com.darkbladedev.engine.listener;

import com.darkbladedev.engine.api.event.MultiblockBreakEvent;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.DisplayNameConfig;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class MultiblockListenerBreakEventTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void breakEventIsFiredAndInstanceDestroyedWhenNotCancelled() {
        Location loc = new Location(world, 10, 64, 10);

        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);

        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        Consumer<Event> caller = events::add;
        MultiblockListener listener = new MultiblockListener(manager, caller);

        Block block = world.getBlockAt(10, 64, 10);
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);

        listener.onBlockBreak(breakEvent);

        assertTrue(manager.destroyed);
        assertFalse(breakEvent.isCancelled());
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MultiblockBreakEvent);
        MultiblockBreakEvent fired = (MultiblockBreakEvent) events.get(0);
        assertSame(instance, fired.getMultiblock());
        assertFalse(fired.isCancelled());
    }

    @Test
    void breakCancellationPreventsDestroy() {
        Location loc = new Location(world, 10, 64, 10);

        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);

        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        Consumer<Event> caller = e -> {
            events.add(e);
            if (e instanceof MultiblockBreakEvent mbe) {
                mbe.setCancelled(true);
            }
        };
        MultiblockListener listener = new MultiblockListener(manager, caller);

        Block block = world.getBlockAt(10, 64, 10);
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);

        listener.onBlockBreak(breakEvent);

        assertFalse(manager.destroyed);
        assertTrue(breakEvent.isCancelled());
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MultiblockBreakEvent);
        assertTrue(((MultiblockBreakEvent) events.get(0)).isCancelled());
    }

    private static MultiblockType dummyType(String id) {
        return new MultiblockType(
                id,
                "1.0",
                new Vector(0, 0, 0),
                block -> false,
                List.of(),
                false,
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );
    }

    private static final class TestManager extends MultiblockManager {
        private MultiblockInstance instance;
        private boolean destroyed;

        private TestManager(MultiblockInstance instance) {
            this.instance = instance;
        }

        @Override
        public Optional<MultiblockInstance> getInstanceAt(Location loc) {
            return Optional.ofNullable(instance);
        }

        @Override
        public void destroyInstance(MultiblockInstance instance) {
            this.destroyed = true;
            this.instance = null;
        }
    }
}
