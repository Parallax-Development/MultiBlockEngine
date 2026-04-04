package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class BlueprintHeldItemResolver {
    private final ItemStackBridge itemStackBridge;

    public BlueprintHeldItemResolver(ItemStackBridge itemStackBridge) {
        this.itemStackBridge = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
    }

    public Optional<ItemStack> findBlueprint(Player player) {
        if (player == null || player.getInventory() == null) {
            return Optional.empty();
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isBlueprint(main)) {
            return Optional.of(main);
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isBlueprint(off)) {
            return Optional.of(off);
        }
        return Optional.empty();
    }

    public Optional<String> blueprintId(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        try {
            var instance = itemStackBridge.fromItemStack(stack);
            if (instance == null || instance.definition() == null || !BlueprintItem.BLUEPRINT_KEY.equals(instance.definition().key())) {
                return Optional.empty();
            }
            Object raw = instance.data().get(BlueprintItem.DATA_STRUCTURE_ID);
            if (!(raw instanceof String id) || id.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(id);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private boolean isBlueprint(ItemStack stack) {
        return blueprintId(stack).isPresent();
    }
}
