package dev.darkblade.mbe.api.item;

public interface ItemService {

    ItemRegistry registry();

    ItemFactory factory();

    ItemModifierRegistry<org.bukkit.command.CommandSender> modifiers();
}

