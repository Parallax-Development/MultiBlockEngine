package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HologramService {
    
    private final Map<MultiblockInstance, Entity> holograms = new ConcurrentHashMap<>();
    private final Set<MultiblockInstance> pendingSpawns = ConcurrentHashMap.newKeySet();
    
    public void spawnHologram(MultiblockInstance instance) {
        if (instance.type().displayName() == null || !instance.type().displayName().visible()) return;
        
        // Prevent concurrent spawns for the same instance
        if (!pendingSpawns.add(instance)) return;
        
        // Remove existing if any
        removeHologram(instance);
        
        Location loc = instance.anchorLocation().clone().add(0.5, 1.5, 0.5); // Default above block
        
        if (loc.getWorld() == null || !loc.getChunk().isLoaded()) {
            pendingSpawns.remove(instance);
            return;
        }
        
        Bukkit.getScheduler().runTask(MultiBlockEngine.getInstance(), () -> {
            try {
                // Ensure instance is still active before spawning
                if (!MultiBlockEngine.getInstance().getManager().isInstanceActive(instance)) {
                    return;
                }

                TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class);
                
                String rawText = instance.type().displayName().text();
                String resolvedText = rawText;

                dev.darkblade.mbe.api.i18n.I18nService i18n = MultiBlockEngine.getInstance().getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.i18n.I18nService.class);
                if (i18n != null && rawText != null && rawText.contains(":")) {
                    try {
                        int idx = rawText.indexOf(':');
                        String origin = rawText.substring(0, idx).trim();
                        String path = rawText.substring(idx + 1).trim();
                        dev.darkblade.mbe.api.i18n.MessageKey key = dev.darkblade.mbe.api.i18n.MessageKey.of(origin, path);
                        // For holograms (entities in the world), we use the server's default locale
                        java.util.Locale locale = i18n.localeProvider() != null ? i18n.localeProvider().fallbackLocale() : java.util.Locale.US;
                        String translated = i18n.resolve(key, locale);
                        if (translated != null && !translated.isBlank() && !translated.equals(key.fullKey())) {
                            resolvedText = translated;
                        }
                    } catch (Throwable ignored) {}
                }

                display.text(StringUtil.legacyText(resolvedText));
                display.setBillboard(Display.Billboard.CENTER);
                display.setPersistent(false); // Don't save to disk, we manage it
                
                holograms.put(instance, display);
            } catch (Exception e) {
                CoreLogger core = MultiBlockEngine.getInstance().getLoggingService() != null ? MultiBlockEngine.getInstance().getLoggingService().core() : null;
                if (core != null) {
                    core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.WARN, "Failed to spawn TextDisplay hologram", e, null, Set.of());
                }
            } finally {
                pendingSpawns.remove(instance);
            }
        });
    }
    
    public void removeHologram(MultiblockInstance instance) {
        pendingSpawns.remove(instance);
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
