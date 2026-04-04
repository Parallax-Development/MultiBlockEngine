package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.api.blueprint.BlueprintService;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.uiengine.InventoryUIService;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class BlueprintServiceImpl implements BlueprintService, MBEService {
    private final BlueprintController controller;
    private final InventoryUIService inventoryUIService;

    public BlueprintServiceImpl(BlueprintController controller, InventoryUIService inventoryUIService) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.inventoryUIService = Objects.requireNonNull(inventoryUIService, "inventoryUIService");
    }

    @Override
    public void startPlacement(Player player, MultiblockDefinition definition) {
        controller.startPlacement(player, definition);
    }

    @Override
    public void openCatalog(Player player) {
        inventoryUIService.open(player, "blueprint_catalog");
    }

    @Override
    public String getServiceId() {
        return "mbe:blueprint";
    }
}
