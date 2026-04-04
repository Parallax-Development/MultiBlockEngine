package dev.darkblade.mbe.catalog;

import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.blueprint.BlueprintItem;
import dev.darkblade.mbe.blueprint.BuildContextService;
import dev.darkblade.mbe.blueprint.Mode;
import dev.darkblade.mbe.blueprint.PlayerBuildContext;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.preview.PreviewSession;
import dev.darkblade.mbe.preview.PreviewState;
import dev.darkblade.mbe.preview.StructurePreviewService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private final CatalogMenu menu;
    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;
    private final StructurePreviewService previewService;
    private final PreviewOriginResolver originResolver;
    private final BuildContextService contextService;
    private final Map<UUID, Instant> openClicks = new ConcurrentHashMap<>();

    public CatalogListener(
        CatalogMenu menu,
        ItemService itemService,
        ItemStackBridge itemStackBridge,
        StructurePreviewService previewService,
        PreviewOriginResolver originResolver,
        BuildContextService contextService
    ) {
        this.menu = menu;
        this.itemService = itemService;
        this.itemStackBridge = itemStackBridge;
        this.previewService = previewService;
        this.originResolver = originResolver;
        this.contextService = contextService;
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
        menu.open(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatalogClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof CatalogMenu.MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (rawSlot == CatalogMenu.PREV_SLOT) {
            menu.open(player, holder.page() - 1);
            return;
        }
        if (rawSlot == CatalogMenu.NEXT_SLOT) {
            menu.open(player, holder.page() + 1);
            return;
        }
        if (rawSlot == 49) {
            player.closeInventory();
            return;
        }
        MultiblockDefinition definition = holder.entries().get(rawSlot);
        if (definition == null) {
            return;
        }
        player.closeInventory();
        ItemStack blueprint = BlueprintItem.create(itemService, itemStackBridge, definition);
        if (blueprint == null) {
            return;
        }
        player.getInventory().setItemInMainHand(blueprint);
        if (previewService.hasActivePreview(player)) {
            previewService.destroyPreview(player);
        }
        PreviewSession session = previewService.startPreview(player, definition);
        if (session == null) {
            contextService.clear(player);
            return;
        }
        session.state(PreviewState.MOVING);
        PlayerBuildContext context = contextService.get(player);
        context.mode(Mode.PREVIEW_PLACEMENT);
        context.preview(session);
        context.blueprintId(definition.id());
        Location origin = originResolver.resolve(player);
        if (origin != null) {
            Location snapped = origin.getBlock().getLocation();
            previewService.updatePreviewOrigin(player, snapped);
            context.lastResolvedOrigin(snapped);
        } else {
            context.lastResolvedOrigin(null);
        }
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
