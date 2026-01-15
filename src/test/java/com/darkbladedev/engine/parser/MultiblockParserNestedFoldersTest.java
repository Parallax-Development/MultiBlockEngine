package com.darkbladedev.engine.parser;

import com.darkbladedev.engine.api.impl.MultiblockAPIImpl;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogBackend;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LoggingConfig;
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

