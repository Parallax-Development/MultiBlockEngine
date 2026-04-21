package dev.darkblade.mbe.core.block;

import dev.darkblade.mbe.api.block.BlockDefinition;
import dev.darkblade.mbe.api.block.BlockKey;
import dev.darkblade.mbe.api.block.BlockRegistry;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlacementListener implements Listener {

    private final BlockItemService blockItemService;
    private final BlockRegistry blockRegistry;
    private final MultiblockRuntimeService runtimeService;
    private final AssemblyCoordinator assemblyCoordinator;

    public BlockPlacementListener(
            BlockItemService blockItemService,
            BlockRegistry blockRegistry,
            MultiblockRuntimeService runtimeService,
            AssemblyCoordinator assemblyCoordinator
    ) {
        this.blockItemService = blockItemService;
        this.blockRegistry = blockRegistry;
        this.runtimeService = runtimeService;
        this.assemblyCoordinator = assemblyCoordinator;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        BlockKey key = blockItemService.getBlockKey(event.getItemInHand());
        if (key == null) return;

        BlockDefinition def = blockRegistry.get(key);
        if (def == null) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        MultiblockType type = runtimeService.getType(key.toString()).orElse(null);
        if (type == null) {
            type = BlockToMultiblockAdapter.adapt(def);
            try {
                runtimeService.registerType(type, new dev.darkblade.mbe.core.domain.MultiblockSource(dev.darkblade.mbe.core.domain.MultiblockSource.Type.CORE_DEFAULT, "builtin"));
            } catch (Exception ignored) {
            }
        }

        boolean autoAssemble = "auto".equalsIgnoreCase(def.assemblyTrigger());

        if (autoAssemble) {
            final MultiblockType fType = type;
            org.bukkit.Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    MultiblockInstance instance = new MultiblockInstance(fType, block.getLocation(), org.bukkit.block.BlockFace.NORTH);
                    if (fType.defaultVariables() != null) {
                        for (String varKey : fType.defaultVariables().keySet()) {
                            instance.setVariable(varKey, fType.defaultVariables().get(varKey));
                        }
                    }
                    runtimeService.registerInstance(instance, true);
                }
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        MultiblockInstance instance = runtimeService.getInstanceAt(loc).orElse(null);
        if (instance != null) {
            BlockKey key = null;
            try {
                key = BlockKey.of(instance.type().id());
            } catch (Exception ignored) {
            }
            if (key != null && blockRegistry.exists(key)) {
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    event.setDropItems(false);
                    org.bukkit.inventory.ItemStack itemToDrop = blockItemService.createItemStack(key);
                    if (itemToDrop != null) {
                        loc.getWorld().dropItemNaturally(loc, itemToDrop);
                    }
                }
            }
        }
    }
}
