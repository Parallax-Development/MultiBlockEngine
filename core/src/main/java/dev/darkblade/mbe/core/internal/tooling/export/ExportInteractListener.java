package dev.darkblade.mbe.core.internal.tooling.export;

import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public final class ExportInteractListener implements Listener {

    private final SelectionService selections;

    public ExportInteractListener(SelectionService selections) {
        this.selections = Objects.requireNonNull(selections, "selections");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        ExportSession session = selections.session(player);
        if (session == null) {
            return;
        }
        String role = session.pendingRole();
        if (role == null || role.isBlank()) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getWorld() == null) {
            return;
        }

        session.markRole(clicked.getLocation(), role);
        session.clearPendingRole();
        PlayerMessageService messageService = resolveMessageService();
        if (messageService != null) {
            messageService.send(player, new PlayerMessage(
                    CoreMessageKeys.EXPORT_MARKED,
                    MessageChannel.CHAT,
                    MessagePriority.NORMAL,
                    java.util.Map.of(
                    "role", role,
                    "x", clicked.getX(),
                    "y", clicked.getY(),
                    "z", clicked.getZ()
                    )
            ));
        }
        event.setCancelled(true);
    }

    private PlayerMessageService resolveMessageService() {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }
}
