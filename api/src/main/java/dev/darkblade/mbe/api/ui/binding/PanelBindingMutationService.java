package dev.darkblade.mbe.api.ui.binding;

import org.bukkit.block.Block;

public interface PanelBindingMutationService {
    boolean unlinkByBlock(Block block);
    int unlinkByWorld(String worldName);
}
