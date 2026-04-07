package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.editor.EditorInput;
import dev.darkblade.mbe.api.editor.EditorSession;
import dev.darkblade.mbe.api.editor.EditorSessionType;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.editor.EditorSessionManager;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public final class LinkPanelSession implements EditorSession {
    private final UUID playerId;
    private final String panelId;
    private final EditorSessionManager sessions;
    private final PanelBindingService bindings;
    private final MultiblockRuntimeService multiblocks;

    public LinkPanelSession(UUID playerId, String panelId, EditorSessionManager sessions, PanelBindingService bindings, MultiblockRuntimeService multiblocks) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.panelId = Objects.requireNonNull(panelId, "panelId");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.multiblocks = Objects.requireNonNull(multiblocks, "multiblocks");
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public EditorSessionType getType() {
        return EditorSessionType.PANEL_LINK;
    }

    @Override
    public void handleInput(EditorInput input) {
        if (input == null) {
            return;
        }
        if (input.type() == EditorInput.Type.CHAT_INPUT) {
            String message = input.payloadAs(String.class).orElse("");
            if (message.equalsIgnoreCase("cancel")) {
                sessions.cancelSession(playerId);
            }
            return;
        }
        if (input.type() != EditorInput.Type.BLOCK_CLICK) {
            return;
        }
        Block block = input.payloadAs(Block.class).orElse(null);
        if (block == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            sessions.cancelSession(playerId);
            return;
        }
        MultiblockInstance instance = multiblocks.getInstanceAt(block.getLocation()).orElse(null);
        if (instance == null || !isController(block.getLocation(), instance.anchorLocation())) {
            send(player, CoreMessageKeys.LINK_INVALID_BLOCK);
            return;
        }
        if (bindings.createBinding(panelId, block, "click").isEmpty()) {
            send(player, CoreMessageKeys.LINK_ALREADY_EXISTS);
            return;
        }
        send(player, CoreMessageKeys.LINK_CREATED, java.util.Map.of("panel", panelId));
        sessions.endSession(playerId);
    }

    @Override
    public void finish() {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            send(player, CoreMessageKeys.LINK_FINISHED);
        }
    }

    @Override
    public void cancel() {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            send(player, CoreMessageKeys.LINK_CANCELLED);
        }
    }

    private void send(Player player, CoreMessageKeys key) {
        I18nService i18n = resolveI18n();
        if (i18n != null) {
            i18n.send(player, key);
        }
    }

    private void send(Player player, CoreMessageKeys key, java.util.Map<String, ?> params) {
        I18nService i18n = resolveI18n();
        if (i18n != null) {
            i18n.send(player, key, params);
        }
    }

    private I18nService resolveI18n() {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(I18nService.class);
    }

    private boolean isController(Location clicked, Location anchor) {
        if (clicked == null || anchor == null || clicked.getWorld() == null || anchor.getWorld() == null) {
            return false;
        }
        return clicked.getWorld().getUID().equals(anchor.getWorld().getUID())
                && clicked.getBlockX() == anchor.getBlockX()
                && clicked.getBlockY() == anchor.getBlockY()
                && clicked.getBlockZ() == anchor.getBlockZ();
    }
}
