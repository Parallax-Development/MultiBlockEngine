package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.api.tool.ActionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

public final class BlueprintInputListener implements Listener {
    private final BlueprintController controller;

    public BlueprintInputListener(BlueprintController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!controller.handleInput(player)) {
            return;
        }

        ActionTrigger trigger = resolveTrigger(event);
        if (trigger == ActionTrigger.SHIFT_RIGHT_CLICK) {
            if (controller.handleRotation(player)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            controller.handleLeftClick(player);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            controller.handleRightClick(player);
        }
    }

    private ActionTrigger resolveTrigger(PlayerInteractEvent event) {
        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR;
        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR;

        if (isRightClick && event.getPlayer().isSneaking()) {
            return ActionTrigger.SHIFT_RIGHT_CLICK;
        }
        if (isLeftClick && event.getPlayer().isSneaking()) {
            return ActionTrigger.SHIFT_LEFT_CLICK;
        }
        if (isRightClick) {
            return ActionTrigger.RIGHT_CLICK;
        }
        return ActionTrigger.LEFT_CLICK;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        controller.handleHeldItem(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        controller.refreshState(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            controller.refreshState(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            controller.refreshState(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            controller.refreshState(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        controller.cancel(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        controller.cancel(event.getPlayer());
    }
}
