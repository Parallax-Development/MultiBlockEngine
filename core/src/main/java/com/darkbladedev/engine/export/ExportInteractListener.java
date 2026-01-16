package com.darkbladedev.engine.export;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public final class ExportInteractListener implements Listener {

    private final SelectionManager selections;

    public ExportInteractListener(SelectionManager selections) {
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
        player.sendMessage(Component.text("Marcado: " + role + " en " + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ(), NamedTextColor.GREEN));
        event.setCancelled(true);
    }
}

