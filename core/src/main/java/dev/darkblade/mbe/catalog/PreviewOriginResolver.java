package dev.darkblade.mbe.catalog;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface PreviewOriginResolver {
    Location resolve(Player player);
}
