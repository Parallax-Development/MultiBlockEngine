package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.service.MBEService;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.darkblade.mbe.api.tick.Tickable;

public final class ToolSessionService implements MBEService, Tickable {

    private final ConcurrentHashMap<SessionKey, SessionValue> sessions = new ConcurrentHashMap<>();
    private final Duration ttl;

    public ToolSessionService() {
        this(Duration.ofSeconds(30));
    }

    public ToolSessionService(Duration ttl) {
        this.ttl = ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(30) : ttl;
    }

    @Override
    public String getServiceId() {
        return "mbe:tool.session";
    }

    public Optional<Map<String, Object>> get(UUID playerId, String modeId) {
        purgeExpired();
        SessionKey key = SessionKey.of(playerId, modeId);
        if (key == null) {
            return Optional.empty();
        }
        SessionValue value = sessions.get(key);
        if (value == null || value.expiresAt.isBefore(Instant.now())) {
            sessions.remove(key);
            return Optional.empty();
        }
        return Optional.of(value.metadata);
    }

    public void put(UUID playerId, String modeId, Map<String, Object> metadata) {
        SessionKey key = SessionKey.of(playerId, modeId);
        if (key == null) {
            return;
        }
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        sessions.put(key, new SessionValue(safeMetadata, Instant.now().plus(ttl)));
    }

    public void clear(UUID playerId, String modeId) {
        SessionKey key = SessionKey.of(playerId, modeId);
        if (key != null) {
            sessions.remove(key);
        }
    }

    public void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        sessions.keySet().removeIf(key -> playerId.equals(key.playerId));
    }

    @Override
    public void tick() {
        purgeExpired();
    }

    @Override
    public int getTickInterval() {
        return 600; // Every 30 seconds
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }

    private record SessionKey(UUID playerId, String modeId) {
        private static SessionKey of(UUID playerId, String modeId) {
            if (playerId == null || modeId == null || modeId.isBlank()) {
                return null;
            }
            return new SessionKey(playerId, modeId.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private record SessionValue(Map<String, Object> metadata, Instant expiresAt) {
    }
}
