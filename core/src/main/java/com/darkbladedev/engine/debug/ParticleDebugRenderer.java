package com.darkbladedev.engine.debug;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.PatternEntry;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleDebugRenderer implements DebugRenderer {
    
    private final MultiBlockEngine plugin;
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();

    public ParticleDebugRenderer(MultiBlockEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(DebugSession session) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (session.isExpired() || !session.player().isOnline()) {
                    stop(session);
                    return;
                }
                render(session);
            }
        };
        task.runTaskTimer(plugin, 0L, 20L); // Render every second
        tasks.put(session.id(), task);
    }

    @Override
    public void stop(DebugSession session) {
        BukkitRunnable task = tasks.remove(session.id());
        if (task != null) {
            task.cancel();
        }
    }

    private void render(DebugSession session) {
        Location anchor = session.anchor();
        // Render controller
        session.player().spawnParticle(Particle.REDSTONE, anchor.clone().add(0.5, 0.5, 0.5), 1, 
            new Particle.DustOptions(Color.BLUE, 1.5f));

        // Render pattern
        for (PatternEntry entry : session.type().pattern()) {
            Vector offset = entry.offset();
            Location target = anchor.clone().add(offset);
            BlockMatcher matcher = entry.matcher();
            
            boolean match = matcher.matches(target.getBlock());
            Color color = match ? Color.GREEN : Color.RED;
            
            session.player().spawnParticle(Particle.REDSTONE, target.add(0.5, 0.5, 0.5), 1, 
                new Particle.DustOptions(color, 1.0f));
        }
    }
}
