package com.darkbladedev.engine.integration;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.manager.MetricsManager;
import com.darkbladedev.engine.manager.MultiblockManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiblockExpansion extends PlaceholderExpansion {

    private final MultiBlockEngine plugin;

    public MultiblockExpansion(MultiBlockEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mbe";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        MultiblockManager manager = plugin.getManager();
        MetricsManager metrics = manager.getMetrics();
        
        if (params.equalsIgnoreCase("types_count")) {
            return String.valueOf(manager.getTypes().size());
        }
        
        if (params.equalsIgnoreCase("created_count")) {
            return String.valueOf(metrics.getCreatedInstances());
        }
        
        if (params.equalsIgnoreCase("destroyed_count")) {
            return String.valueOf(metrics.getDestroyedInstances());
        }
        
        if (params.equalsIgnoreCase("avg_tick_ms")) {
            return String.format("%.2f", metrics.getAverageTickTimeMs());
        }
        
        return null;
    }
}
