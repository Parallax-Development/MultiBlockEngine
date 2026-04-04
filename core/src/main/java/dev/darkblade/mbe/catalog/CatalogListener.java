package dev.darkblade.mbe.catalog;

import dev.darkblade.mbe.api.blueprint.BlueprintService;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CatalogListener implements Listener {
    private static final ItemKey WRENCH_KEY = ItemKeys.of("mbe:wrench", 0);
    private static final Duration OPEN_DEBOUNCE = Duration.ofMillis(300);
    private final BlueprintService blueprintService;
    private final ItemStackBridge itemStackBridge;
    private final Map<UUID, Instant> openClicks = new ConcurrentHashMap<>();

    public CatalogListener(
        BlueprintService blueprintService,
        ItemStackBridge itemStackBridge
    ) {
        this.blueprintService = blueprintService;
        this.itemStackBridge = itemStackBridge;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWrenchInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !player.isSneaking()) {
            return;
        }
        if (!isWrench(event.getItem())) {
            return;
        }
        if (!acquireOpen(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        blueprintService.openCatalog(player);
    }

    private boolean acquireOpen(UUID playerId) {
        Instant now = Instant.now();
        Instant last = openClicks.put(playerId, now);
        if (last == null) {
            return true;
        }
        return Duration.between(last, now).compareTo(OPEN_DEBOUNCE) > 0;
    }

    private boolean isWrench(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemInstance instance;
        try {
            instance = itemStackBridge.fromItemStack(stack);
        } catch (Throwable t) {
            return false;
        }
        if (instance == null || instance.definition() == null) {
            return false;
        }
        ItemKey key = instance.definition().key();
        return WRENCH_KEY.equals(key);
    }
}
