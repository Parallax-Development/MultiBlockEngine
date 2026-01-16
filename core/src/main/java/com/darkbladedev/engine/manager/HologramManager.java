package com.darkbladedev.engine.manager;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {
    
    private final Map<MultiblockInstance, Entity> holograms = new ConcurrentHashMap<>();
    
    public void spawnHologram(MultiblockInstance instance) {
        if (instance.type().displayName() == null || !instance.type().displayName().visible()) return;
        
        // Remove existing if any
        removeHologram(instance);
        
        Location loc = instance.anchorLocation().clone().add(0.5, 1.5, 0.5); // Default above block
        // We could add an offset config later
        
        if (loc.getWorld() == null || !loc.getChunk().isLoaded()) return; // Safety
        
        Bukkit.getScheduler().runTask(MultiBlockEngine.getInstance(), () -> {
            // Ensure instance is still active before spawning
            if (!MultiBlockEngine.getInstance().getManager().isInstanceActive(instance)) {
                return;
            }

            try {
                TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class);
                
                String rawText = instance.type().displayName().text();

                display.text(StringUtil.legacyText(rawText));
                display.setBillboard(Display.Billboard.CENTER);
                display.setPersistent(false); // Don't save to disk, we manage it
                
                holograms.put(instance, display);
            } catch (Exception e) {
                CoreLogger core = MultiBlockEngine.getInstance().getLoggingManager() != null ? MultiBlockEngine.getInstance().getLoggingManager().core() : null;
                if (core != null) {
                    core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.WARN, "Failed to spawn TextDisplay hologram", e, null, Set.of());
                } else {
                    MultiBlockEngine.getInstance().getLogger().warning("Failed to spawn TextDisplay hologram. Ensure 1.19.4+");
                }
            }
        });
    }
    
    public void removeHologram(MultiblockInstance instance) {
        Entity entity = holograms.remove(instance);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }
    
    public void removeAll() {
        for (Entity entity : holograms.values()) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        holograms.clear();
    }
}
