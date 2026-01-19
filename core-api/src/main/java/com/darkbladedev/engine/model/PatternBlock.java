package com.darkbladedev.engine.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record PatternBlock(Material material, Map<String, String> requiredProperties) {

    public PatternBlock {
        Objects.requireNonNull(material, "material");

        Map<String, String> normalized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (requiredProperties != null) {
            for (Map.Entry<String, String> e : requiredProperties.entrySet()) {
                if (e == null || e.getKey() == null) {
                    continue;
                }
                String k = e.getKey().trim().toLowerCase(Locale.ROOT);
                if (k.isEmpty()) {
                    continue;
                }
                String v = e.getValue() == null ? "" : e.getValue().trim().toLowerCase(Locale.ROOT);
                normalized.put(k, v);
            }
        }
        requiredProperties = Map.copyOf(normalized);
    }

    @Override
    public String toString() {
        String base = material.getKey() != null ? material.getKey().toString() : material.name().toLowerCase(Locale.ROOT);
        if (requiredProperties.isEmpty()) {
            return base;
        }
        String props = requiredProperties.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(","));
        return base + "[" + props + "]";
    }
}

