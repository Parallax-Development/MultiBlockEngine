package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.catalog.StructureCatalogService;
import dev.darkblade.mbe.preview.MultiblockDefinition;

import java.util.Objects;

public final class BlueprintDefinitionResolver {
    private final StructureCatalogService catalogService;

    public BlueprintDefinitionResolver(StructureCatalogService catalogService) {
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
    }

    public MultiblockDefinition resolve(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (MultiblockDefinition definition : catalogService.getAll()) {
            if (definition == null || definition.id() == null) {
                continue;
            }
            if (id.equals(definition.id())) {
                return definition;
            }
        }
        return null;
    }
}
