package dev.darkblade.mbe.uiengine;

import dev.darkblade.mbe.catalog.StructureCatalogService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public final class BlueprintDataProvider implements InventoryDataProvider {
    private final StructureCatalogService catalogService;

    public BlueprintDataProvider(StructureCatalogService catalogService) {
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
    }

    @Override
    public List<?> provide(Player player) {
        return catalogService.getAll();
    }
}
