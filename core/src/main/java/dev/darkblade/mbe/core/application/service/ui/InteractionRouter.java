package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.api.ui.binding.PanelBinding;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionRouter implements Listener {
    private final Map<BlockPosition, PanelBinding> clickBindings = new ConcurrentHashMap<>();

    public void registerClickBinding(PanelBinding binding) {
        if (binding == null) {
            return;
        }
        clickBindings.put(new BlockPosition(binding.world(), binding.x(), binding.y(), binding.z()), binding);
    }

    public void unregisterClickBinding(PanelBinding binding) {
        if (binding == null) {
            return;
        }
        clickBindings.remove(new BlockPosition(binding.world(), binding.x(), binding.y(), binding.z()));
    }

    public Optional<PanelBinding> getClickBinding(Block block) {
        return BlockPosition.fromBlock(block).map(clickBindings::get).filter(Objects::nonNull);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block clicked = event.getClickedBlock();
        Player player = event.getPlayer();
        if (clicked == null || player == null) {
            return;
        }
        Optional<PanelBinding> bindingOpt = getClickBinding(clicked);
        if (bindingOpt.isEmpty()) {
            return;
        }
        PanelViewService panelService = resolvePanelService();
        if (panelService == null) {
            return;
        }
        PanelBinding binding = bindingOpt.get();
        panelService.openPanel(player, binding.panelId());
        event.setCancelled(true);
    }

    private PanelViewService resolvePanelService() {
        return PanelViewResolver.resolve();
    }
}
