package dev.darkblade.mbe.api.ui.binding;

import org.bukkit.block.Block;

public interface PanelBindingLinkService {
    boolean linkPanelToBlock(String panelId, Block block, String triggerType);
}
