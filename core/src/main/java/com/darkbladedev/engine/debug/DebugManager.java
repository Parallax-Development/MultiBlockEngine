package com.darkbladedev.engine.debug;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.model.MultiblockType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DebugManager {
    private final MultiBlockEngine plugin;
    private final Map<UUID, DebugSession> sessions = new ConcurrentHashMap<>();
    private final DebugRenderer renderer;

    public DebugManager(MultiBlockEngine plugin) {
        this.plugin = plugin;
        // In a real implementation, this would be chosen from config
        this.renderer = new ParticleDebugRenderer(plugin); 
    }

    public void startSession(Player player, MultiblockType type, Location anchor) {
        // Clean existing session for player
        stopSession(player);

        long duration = plugin.getConfig().getLong("debug.duration", 10) * 1000;
        
        DebugSession session = new DebugSession(
            player.getUniqueId(),
            player,
            type,
            anchor,
            System.currentTimeMillis(),
            duration
        );
        
        sessions.put(player.getUniqueId(), session);
        renderer.start(session);
        player.sendMessage("§aDebug session started for " + type.id());
    }

    public void stopSession(Player player) {
        DebugSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            renderer.stop(session);
        }
    }
    
    public void stopAll() {
        for (DebugSession session : sessions.values()) {
            renderer.stop(session);
        }
        sessions.clear();
    }
}
