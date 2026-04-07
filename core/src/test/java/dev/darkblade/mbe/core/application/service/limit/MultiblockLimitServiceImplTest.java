package dev.darkblade.mbe.core.application.service.limit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import dev.darkblade.mbe.api.persistence.PersistentStorageService;
import dev.darkblade.mbe.core.infrastructure.persistence.FilePersistentStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockLimitServiceImplTest {

    private ServerMock server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            MockBukkit.unmock();
            server = null;
        }
    }

    @Test
    void assembleWithinLimitThenBlocksWhenExceeded(@TempDir Path dir) {
        server = MockBukkit.mock();
        PlayerMock player = server.addPlayer();

        PersistentStorageService persistence = new FilePersistentStorageService(dir.resolve("persist"));
        persistence.initialize();
        MultiblockLimitResolver resolver = (p, id) -> Optional.of(new MultiblockLimitDefinition("mbe.limit.default", 2, LimitScope.GLOBAL, null));
        MultiblockLimitService service = new MultiblockLimitServiceImpl(persistence, resolver);

        assertTrue(service.canAssemble(player, "test:generator"));
        service.registerAssembly(player, "test:generator");
        assertTrue(service.canAssemble(player, "test:generator"));
        service.registerAssembly(player, "test:generator");
        assertFalse(service.canAssemble(player, "test:generator"));
    }

    @Test
    void breakReleasesSlot(@TempDir Path dir) {
        server = MockBukkit.mock();
        PlayerMock player = server.addPlayer();

        PersistentStorageService persistence = new FilePersistentStorageService(dir.resolve("persist"));
        persistence.initialize();
        MultiblockLimitResolver resolver = (p, id) -> Optional.of(new MultiblockLimitDefinition("mbe.limit.default", 1, LimitScope.GLOBAL, null));
        MultiblockLimitService service = new MultiblockLimitServiceImpl(persistence, resolver);

        service.registerAssembly(player, "test:generator");
        assertFalse(service.canAssemble(player, "test:generator"));
        service.unregisterAssembly(player, "test:generator");
        assertTrue(service.canAssemble(player, "test:generator"));
    }

    @Test
    void countersPersistAcrossRestart(@TempDir Path dir) {
        UUID playerId = UUID.randomUUID();
        String multiblockId = "test:generator";
        MultiblockLimitDefinition def = new MultiblockLimitDefinition("mbe.limit.default", 1, LimitScope.GLOBAL, null);

        PersistentStorageService persistenceA = new FilePersistentStorageService(dir.resolve("persist"));
        persistenceA.initialize();
        MultiblockLimitService serviceA = new MultiblockLimitServiceImpl(persistenceA, (p, id) -> Optional.of(def));
        serviceA.registerAssembly(playerId, multiblockId);
        persistenceA.flush();
        persistenceA.shutdown(true);

        PersistentStorageService persistenceB = new FilePersistentStorageService(dir.resolve("persist"));
        persistenceB.initialize();
        MultiblockLimitService serviceB = new MultiblockLimitServiceImpl(persistenceB, (p, id) -> Optional.of(def));
        assertFalse(serviceB.canAssemble(playerId, multiblockId, def));
        persistenceB.shutdown(true);
    }
}
