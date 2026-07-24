package dev.darkblade.mbe.api.tool;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ToolSessionService {
    Optional<Map<String, Object>> get(UUID playerId, String modeId);

    void put(UUID playerId, String modeId, Map<String, Object> metadata);

    void clear(UUID playerId, String modeId);

    void clearPlayer(UUID playerId);
}
