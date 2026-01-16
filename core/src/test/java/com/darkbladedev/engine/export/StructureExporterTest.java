package com.darkbladedev.engine.export;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import com.darkbladedev.engine.api.export.ExportHookRegistry;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogBackend;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LoggingConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StructureExporterTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void exportSimple() {
        world.getBlockAt(0, 64, 0).setType(Material.IRON_BLOCK);
        world.getBlockAt(1, 64, 0).setType(Material.STONE);

        ExportHookRegistry hooks = new DefaultExportHookRegistry();
        StructureExporter exporter = new StructureExporter(testLogger(), hooks, new ExportConfig(false, true, false, false, Set.of()));

        ExportSession session = new ExportSession(java.util.UUID.randomUUID());
        session.setPos1(new Location(world, 0, 64, 0));
        session.setPos2(new Location(world, 1, 64, 0));
        session.markRole(new Location(world, 0, 64, 0), "controller");

        StructureExporter.ExportResult res = exporter.exportToYaml("test_machine", session);
        assertTrue(res.hasController());
        assertEquals("test_machine", res.id());
        assertTrue(res.yaml().contains("controller: IRON_BLOCK"));
        assertTrue(res.yaml().contains("- offset: [1, 0, 0]"));
        assertTrue(res.yaml().contains("match: STONE"));
    }

    @Test
    void exportWithoutControllerFails() {
        world.getBlockAt(0, 64, 0).setType(Material.IRON_BLOCK);

        ExportHookRegistry hooks = new DefaultExportHookRegistry();
        StructureExporter exporter = new StructureExporter(testLogger(), hooks, new ExportConfig(false, true, false, false, Set.of()));

        ExportSession session = new ExportSession(java.util.UUID.randomUUID());
        session.setPos1(new Location(world, 0, 64, 0));
        session.setPos2(new Location(world, 0, 64, 0));

        assertThrows(StructureExporter.ExportException.class, () -> exporter.exportToYaml("test_machine", session));
    }

    @Test
    void hookCanAddPropertiesAndRoles() {
        world.getBlockAt(0, 64, 0).setType(Material.IRON_BLOCK);
        world.getBlockAt(1, 64, 0).setType(Material.STONE);

        DefaultExportHookRegistry hooks = new DefaultExportHookRegistry();
        hooks.register((block, context) -> {
            if (block.material() == Material.STONE) {
                context.markRole(block.pos(), "decorative");
                context.putProperty(block.pos(), "capacity", 128);
            }
        });

        StructureExporter exporter = new StructureExporter(testLogger(), hooks, new ExportConfig(false, true, false, false, Set.of()));

        ExportSession session = new ExportSession(java.util.UUID.randomUUID());
        session.setPos1(new Location(world, 0, 64, 0));
        session.setPos2(new Location(world, 1, 64, 0));
        session.markRole(new Location(world, 0, 64, 0), "controller");

        StructureExporter.ExportResult res = exporter.exportToYaml("test_machine", session);
        assertTrue(res.yaml().contains("roles:"));
        assertTrue(res.yaml().contains("decorative:"));
        assertTrue(res.yaml().contains("properties:"));
        assertTrue(res.yaml().contains("capacity: 128"));
    }

    @Test
    void ignoredBlocksAreSkippedAndWarned() {
        world.getBlockAt(0, 64, 0).setType(Material.IRON_BLOCK);
        world.getBlockAt(1, 64, 0).setType(Material.TORCH);

        ExportHookRegistry hooks = new DefaultExportHookRegistry();
        StructureExporter exporter = new StructureExporter(testLogger(), hooks, new ExportConfig(false, true, false, false, Set.of(Material.TORCH)));

        ExportSession session = new ExportSession(java.util.UUID.randomUUID());
        session.setPos1(new Location(world, 0, 64, 0));
        session.setPos2(new Location(world, 1, 64, 0));
        session.markRole(new Location(world, 0, 64, 0), "controller");

        StructureExporter.ExportResult res = exporter.exportToYaml("test_machine", session);
        assertTrue(res.yaml().contains("warnings:"));
        assertTrue(res.yaml().contains("Block ignorado: TORCH"));
        assertFalse(res.yaml().contains("match: TORCH"));
    }

    private static CoreLogger testLogger() {
        LogBackend backend = entry -> {
        };
        LoggingConfig cfg = new LoggingConfig(LogLevel.INFO, false, false, false, Set.of());
        return new CoreLogger("Test", backend, cfg);
    }
}

