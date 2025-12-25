package com.darkbladedev.engine.listener;

import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class MultiblockListener implements Listener {

    private final MultiblockManager manager;

    public MultiblockListener(MultiblockManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        checkController(block, event.getPlayer());
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            checkController(event.getClickedBlock(), event.getPlayer());
        }
    }

    @SuppressWarnings("deprecation")
    private void checkController(Block block, Player player) {
        // Optimization: In a real plugin, we would have a map of Material -> List<MultiblockType>
        // to avoid iterating all types.
        for (MultiblockType type : manager.getTypes()) {
            if (type.controllerMatcher().matches(block)) {
                // Potential controller.
                // Check if already an instance
                if (manager.getInstanceAt(block.getLocation()).isPresent()) {
                    continue; // Already active
                }
                
                Optional<MultiblockInstance> instance = manager.tryCreate(block, type);
                if (instance.isPresent()) {
                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "Structure formed: " + type.id());
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<MultiblockInstance> instance = manager.getInstanceAt(block.getLocation());
        if (instance.isPresent()) {
            manager.destroyInstance(instance.get());
            event.getPlayer().sendMessage(ChatColor.RED + "Structure invalidated: " + instance.get().type().id());
        }
    }
}
