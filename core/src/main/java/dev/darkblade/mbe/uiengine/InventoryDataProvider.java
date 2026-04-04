package dev.darkblade.mbe.uiengine;

import org.bukkit.entity.Player;

import java.util.List;

public interface InventoryDataProvider {
    List<?> provide(Player player);
}
