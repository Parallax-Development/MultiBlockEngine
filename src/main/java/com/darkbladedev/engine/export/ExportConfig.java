package com.darkbladedev.engine.export;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record ExportConfig(
        boolean includeAir,
        boolean includeBlockStates,
        boolean includeNbt,
        boolean includeWaterlogged,
        Set<Material> ignoredMaterials
) {
    public ExportConfig {
        ignoredMaterials = ignoredMaterials == null ? Set.of() : Set.copyOf(ignoredMaterials);
    }

    public static ExportConfig from(org.bukkit.configuration.ConfigurationSection section) {
        boolean includeAir = section != null && section.getBoolean("includeAir", false);
        boolean includeBlockStates = section == null || section.getBoolean("includeBlockStates", true);
        boolean includeNbt = section != null && section.getBoolean("includeNbt", false);
        boolean includeWaterlogged = section != null && section.getBoolean("includeWaterlogged", false);
        Set<Material> ignored = parseIgnoredMaterials(section == null ? List.of() : section.getStringList("ignoreMaterials"));
        return new ExportConfig(includeAir, includeBlockStates, includeNbt, includeWaterlogged, ignored);
    }

    private static Set<Material> parseIgnoredMaterials(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        EnumSet<Material> out = EnumSet.noneOf(Material.class);
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }

            Material m;
            try {
                m = Material.valueOf(t.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                m = Material.matchMaterial(t);
            }
            if (m != null) {
                out.add(m);
            }
        }
        return Set.copyOf(out);
    }
}

