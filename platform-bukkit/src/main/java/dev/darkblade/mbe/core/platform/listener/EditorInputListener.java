package dev.darkblade.mbe.core.platform.listener;

import dev.darkblade.mbe.api.editor.EditorInput;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.editor.EditorSessionManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.UUID;

public final class EditorInputListener implements Listener {
    private final MultiBlockEngine plugin;
    private final EditorSessionManager sessions;

    public EditorInputListener(MultiBlockEngine plugin, EditorSessionManager sessions) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event == null || event.getPlayer() == null || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        UUID playerId = event.getPlayer().getUniqueId();
        boolean consumed = sessions.dispatch(playerId, new EditorInput(EditorInput.Type.BLOCK_CLICK, block));
        if (consumed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event == null || event.getPlayer() == null || event.getRightClicked() == null) {
            return;
        }
        Entity entity = event.getRightClicked();
        UUID playerId = event.getPlayer().getUniqueId();
        boolean consumed = sessions.dispatch(playerId, new EditorInput(EditorInput.Type.ENTITY_CLICK, entity));
        if (consumed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event == null || event.getPlayer() == null || event.getMessage() == null) {
            return;
        }
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (sessions.getSession(playerId).isEmpty()) {
            return;
        }
        String message = event.getMessage();
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () ->
                sessions.dispatch(playerId, new EditorInput(EditorInput.Type.CHAT_INPUT, message))
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        sessions.cancelSession(event.getPlayer().getUniqueId());
    }
}
