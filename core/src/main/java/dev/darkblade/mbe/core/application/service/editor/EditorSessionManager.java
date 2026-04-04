package dev.darkblade.mbe.core.application.service.editor;

import dev.darkblade.mbe.api.editor.EditorInput;
import dev.darkblade.mbe.api.editor.EditorSession;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorSessionManager {
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();

    public void startSession(Player player, EditorSession session) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(session, "session");
        UUID playerId = player.getUniqueId();
        EditorSession previous = sessions.put(playerId, session);
        if (previous != null) {
            safeCancel(previous);
        }
    }

    public Optional<EditorSession> getSession(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(playerId));
    }

    public boolean dispatch(UUID playerId, EditorInput input) {
        if (playerId == null || input == null) {
            return false;
        }
        EditorSession session = sessions.get(playerId);
        if (session == null) {
            return false;
        }
        session.handleInput(input);
        return true;
    }

    public void endSession(UUID playerId) {
        if (playerId == null) {
            return;
        }
        EditorSession session = sessions.remove(playerId);
        if (session != null) {
            safeFinish(session);
        }
    }

    public void cancelSession(UUID playerId) {
        if (playerId == null) {
            return;
        }
        EditorSession session = sessions.remove(playerId);
        if (session != null) {
            safeCancel(session);
        }
    }

    public void cancelAll() {
        for (UUID playerId : List.copyOf(sessions.keySet())) {
            cancelSession(playerId);
        }
    }

    private void safeFinish(EditorSession session) {
        try {
            session.finish();
        } catch (Throwable ignored) {
        }
    }

    private void safeCancel(EditorSession session) {
        try {
            session.cancel();
        } catch (Throwable ignored) {
        }
    }
}
