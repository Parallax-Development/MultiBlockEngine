package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.api.ui.binding.PanelBinding;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionRouter {
    private final Map<BlockPosition, PanelBinding> clickBindings = new ConcurrentHashMap<>();
    private volatile PanelViewService panelViewService;

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

    public boolean route(InteractionIntent intent) {
        if (intent == null) {
            return false;
        }
        if (intent.type() != InteractionType.RIGHT_CLICK_BLOCK && intent.type() != InteractionType.SHIFT_RIGHT_CLICK) {
            return false;
        }
        Block clicked = intent.targetBlock();
        Player player = intent.player();
        if (clicked == null || player == null) {
            return false;
        }
        Optional<PanelBinding> bindingOpt = getClickBinding(clicked);
        if (bindingOpt.isEmpty()) {
            return false;
        }
        PanelViewService panelService = panelViewService;
        if (panelService == null) {
            return false;
        }
        PanelBinding binding = bindingOpt.get();
        panelService.openPanel(player, binding.panelId());
        return true;
    }

    public void setPanelViewService(PanelViewService panelViewService) {
        this.panelViewService = panelViewService;
    }
}
