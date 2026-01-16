package com.darkbladedev.engine.parser;

import com.darkbladedev.engine.api.impl.MultiblockAPIImpl;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogBackend;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LoggingConfig;
import com.darkbladedev.engine.api.port.PortBlockRef;
import com.darkbladedev.engine.api.port.PortDirection;
import com.darkbladedev.engine.model.MultiblockSource;
import com.darkbladedev.engine.model.MultiblockType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class MultiblockParserNestedFoldersTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsYamlFilesRecursivelyFromNestedFolders() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve(".default").resolve("magic"));
        Files.createDirectories(multiblocks.resolve("factory"));

        writeYaml(multiblocks.resolve(".default").resolve("magic").resolve("mana_generator.yml"), """
                id: mana_generator
                version: "1.0"
                controller: DIAMOND_BLOCK
                pattern: []
                """);

        writeYaml(multiblocks.resolve("factory").resolve("crusher.yml"), """
                id: crusher
                version: "1.0"
                controller: IRON_BLOCK
                pattern:
                  - offset: [0, 1, 0]
                    match: STONE
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());

        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(t -> t.type().id().equalsIgnoreCase("mana_generator")));
        assertTrue(loaded.stream().anyMatch(t -> t.type().id().equalsIgnoreCase("crusher")));
    }

    @Test
    void assignsSourceTypeBasedOnDefaultFolder() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve(".default"));
        Files.createDirectories(multiblocks.resolve("custom"));

        writeYaml(multiblocks.resolve(".default").resolve("furnace.yml"), """
                id: furnace
                version: "1.0"
                controller: FURNACE
                pattern: []
                """);
        writeYaml(multiblocks.resolve("custom").resolve("smelter.yml"), """
                id: smelter
                version: "1.0"
                controller: BLAST_FURNACE
                pattern: []
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());

        var furnace = loaded.stream().filter(t -> t.type().id().equalsIgnoreCase("furnace")).findFirst().orElseThrow();
        var smelter = loaded.stream().filter(t -> t.type().id().equalsIgnoreCase("smelter")).findFirst().orElseThrow();

        assertEquals(MultiblockSource.Type.CORE_DEFAULT, furnace.source().type());
        assertEquals(MultiblockSource.Type.USER_DEFINED, smelter.source().type());
    }

    @Test
    void duplicateIdsPreferCoreDefault() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve(".default"));
        Files.createDirectories(multiblocks.resolve("custom"));

        writeYaml(multiblocks.resolve(".default").resolve("mana_generator.yml"), """
                id: mana_generator
                version: "1.0"
                controller: DIAMOND_BLOCK
                pattern: []
                """);
        writeYaml(multiblocks.resolve("custom").resolve("mana_generator.yml"), """
                id: mana_generator
                version: "1.0"
                controller: EMERALD_BLOCK
                pattern: []
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());

        assertEquals(1, loaded.size());
        assertEquals("mana_generator", loaded.get(0).type().id());
        assertEquals(MultiblockSource.Type.CORE_DEFAULT, loaded.get(0).source().type());
    }

    @Test
    void derivesIdFromFilenameWhenMissing() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve("custom"));

        writeYaml(multiblocks.resolve("custom").resolve("custom_machine.yml"), """
                version: "1.0"
                controller: IRON_BLOCK
                pattern: []
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());

        assertEquals(1, loaded.size());
        MultiblockType type = loaded.get(0).type();
        assertEquals("custom_machine", type.id());
    }

    @Test
    void parsesPortsAndExtensions() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve("custom"));

        writeYaml(multiblocks.resolve("custom").resolve("with_ports.yml"), """
                id: with_ports
                version: "1.0"
                controller: IRON_BLOCK
                pattern: []
                ports:
                  energy_in:
                    direction: input
                    type: energy
                    block: controller
                    capabilities: [accept]
                  item_out:
                    direction: output
                    type: item
                    block: [1, 0, 0]
                    capabilities:
                      - emit
                extensions:
                  mbe-electricity:
                    storage:
                      capacity: 10000
                      voltage: 220
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());
        assertEquals(1, loaded.size());

        MultiblockType type = loaded.get(0).type();
        assertEquals("with_ports", type.id());
        assertEquals(2, type.ports().size());

        var in = type.ports().get("energy_in");
        assertNotNull(in);
        assertEquals(PortDirection.INPUT, in.direction());
        assertEquals("energy", in.type());
        assertTrue(in.block() instanceof PortBlockRef.Controller);
        assertTrue(in.capabilities().contains("accept"));

        var out = type.ports().get("item_out");
        assertNotNull(out);
        assertEquals(PortDirection.OUTPUT, out.direction());
        assertEquals("item", out.type());
        assertTrue(out.block() instanceof PortBlockRef.Offset);
        PortBlockRef.Offset off = (PortBlockRef.Offset) out.block();
        assertEquals(1, off.dx());
        assertEquals(0, off.dy());
        assertEquals(0, off.dz());

        assertTrue(type.extensions().containsKey("mbe-electricity"));
    }

    @Test
    void parsesLegacyRolesAsPorts() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve("custom"));

        writeYaml(multiblocks.resolve("custom").resolve("legacy_roles.yml"), """
                id: legacy_roles
                version: "1.0"
                controller: IRON_BLOCK
                pattern: []
                roles:
                  input:
                    - [1, 0, 0]
                  output:
                    - [0, 0, 1]
                    - [0, 1, 0]
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());
        assertEquals(1, loaded.size());

        MultiblockType type = loaded.get(0).type();
        assertEquals("legacy_roles", type.id());
        assertEquals(3, type.ports().size());

        assertTrue(type.ports().containsKey("port_in_1"));
        assertTrue(type.ports().containsKey("port_out_1"));
        assertTrue(type.ports().containsKey("port_out_2"));
    }

    @Test
    void parsesLegacyInputsOutputsAsPorts() throws Exception {
        Path multiblocks = tempDir.resolve("multiblocks");
        Files.createDirectories(multiblocks.resolve("custom"));

        writeYaml(multiblocks.resolve("custom").resolve("legacy_io.yml"), """
                id: legacy_io
                version: "1.0"
                controller: IRON_BLOCK
                pattern: []
                inputs:
                  energy:
                    block: controller
                outputs:
                  items:
                    block: [1, 0, 0]
                """);

        MultiblockParser parser = new MultiblockParser(new MultiblockAPIImpl(), testLogger());
        var loaded = parser.loadAllWithSources(multiblocks.toFile());
        assertEquals(1, loaded.size());

        MultiblockType type = loaded.get(0).type();
        assertEquals("legacy_io", type.id());
        assertEquals(2, type.ports().size());

        assertTrue(type.ports().containsKey("input_energy"));
        assertTrue(type.ports().containsKey("output_items"));
    }

    private static void writeYaml(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content.replace("\r\n", "\n"), StandardCharsets.UTF_8);
    }

    private static CoreLogger testLogger() {
        LogBackend backend = entry -> {
        };
        LoggingConfig cfg = new LoggingConfig(LogLevel.INFO, false, false, false, Set.of());
        return new CoreLogger("Test", backend, cfg);
    }
}
