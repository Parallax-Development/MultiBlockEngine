package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public class OpenPanelAction implements dev.darkblade.mbe.core.domain.action.Action {
    private final String panelId;

    public OpenPanelAction(String panelId) {
        this.panelId = panelId;
    }

    @Override
    public void execute(MultiblockInstance instance, Player player) {
        if (player == null) return;
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin != null && plugin.getAddonLifecycleService() != null) {
            // Support opening the special Blueprint Crafting Table panel
            if ("blueprint_crafting_table".equals(panelId)) {
                dev.darkblade.mbe.api.blueprint.BlueprintService blueprintService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.blueprint.BlueprintService.class);
                if (blueprintService != null) {
                    blueprintService.openCraftingTable(player);
                    return;
                }
            }

            // Fallback to Addon PanelViewService
            PanelViewService panelService = plugin.getAddonLifecycleService().getCoreService(PanelViewService.class);
            if (panelService != null && panelService.getPanel(dev.darkblade.mbe.api.ui.runtime.PanelId.of(panelId)).isPresent()) {
                panelService.openPanel(player, panelId);
                return;
            }

            // Fallback to Core InventoryUIService
            dev.darkblade.mbe.uiengine.InventoryUIService inventoryUIService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.uiengine.InventoryUIService.class);
            if (inventoryUIService != null) {
                try {
                    inventoryUIService.open(player, panelId);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public boolean shouldExecuteOnInteract(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK;
    }

    @Override
    public boolean cancelsVanillaOnInteract(Action action) {
        return true;
    }

    @Override
    public String ownerId() {
        return "core";
    }

    @Override
    public String typeKey() {
        return "open_panel";
    }
}
