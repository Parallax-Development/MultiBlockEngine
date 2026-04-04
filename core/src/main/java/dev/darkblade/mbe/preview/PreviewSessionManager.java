package dev.darkblade.mbe.preview;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PreviewSessionManager {
    private final Map<UUID, PreviewSession> sessions = new ConcurrentHashMap<>();

    PreviewSession put(PreviewSession session) {
        return sessions.put(session.playerId(), session);
    }

    PreviewSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    PreviewSession remove(UUID playerId) {
        return sessions.remove(playerId);
    }

    boolean has(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    Collection<PreviewSession> all() {
        return sessions.values();
    }
}
