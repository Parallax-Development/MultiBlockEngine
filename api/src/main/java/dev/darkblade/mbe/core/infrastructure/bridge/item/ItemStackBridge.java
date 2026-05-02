package dev.darkblade.mbe.core.infrastructure.bridge.item;

import dev.darkblade.mbe.api.item.ItemInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public interface ItemStackBridge {

    ItemStack toItemStack(ItemInstance instance);

    ItemStack toItemStack(ItemInstance instance, Locale locale);

    default ItemStack toItemStack(ItemInstance instance, CommandSender sender) {
        if (sender == null) {
            return toItemStack(instance);
        }
        // This is a bit hacky because we don't have the I18nService here,
        // but implementations can override this if they have access to LocaleProvider.
        return toItemStack(instance);
    }

    ItemInstance fromItemStack(ItemStack stack);
}
