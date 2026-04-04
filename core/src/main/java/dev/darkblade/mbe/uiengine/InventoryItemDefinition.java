package dev.darkblade.mbe.uiengine;

import org.bukkit.Material;

import java.util.List;

public record InventoryItemDefinition(
        Material material,
        String name,
        List<String> lore,
        boolean glow
) {
    public InventoryItemDefinition {
        material = material == null ? Material.STONE : material;
        name = name == null ? "" : name;
        lore = lore == null ? List.of() : List.copyOf(lore);
    }
}
