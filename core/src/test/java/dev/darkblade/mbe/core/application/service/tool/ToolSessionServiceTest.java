package dev.darkblade.mbe.core.application.service.tool;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSessionServiceTest {

    @Test
    void storesAndClearsSessionData() {
        ToolSessionService service = new ToolSessionService(Duration.ofSeconds(10));
        UUID playerId = UUID.randomUUID();

        service.put(playerId, "link_ports", Map.of("origin", "x"));
        Optional<Map<String, Object>> found = service.get(playerId, "link_ports");
        assertTrue(found.isPresent());
        assertEquals("x", found.get().get("origin"));

        service.clear(playerId, "link_ports");
        assertTrue(service.get(playerId, "link_ports").isEmpty());
    }
}
