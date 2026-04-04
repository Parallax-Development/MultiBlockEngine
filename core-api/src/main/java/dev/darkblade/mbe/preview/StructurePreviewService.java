package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface StructurePreviewService {
    PreviewSession startPreview(Player player, MultiblockDefinition definition);
    void updatePreviewOrigin(Player player, Location newOrigin);
    void rotatePreview(Player player, Rotation rotation);
    void destroyPreview(Player player);
    boolean hasActivePreview(Player player);
}
