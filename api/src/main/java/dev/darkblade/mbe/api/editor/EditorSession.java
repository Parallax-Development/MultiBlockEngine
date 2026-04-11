package dev.darkblade.mbe.api.editor;

import java.util.UUID;

public interface EditorSession {
    UUID getPlayerId();
    EditorSessionType getType();
    void handleInput(EditorInput input);
    void finish();
    void cancel();
}
