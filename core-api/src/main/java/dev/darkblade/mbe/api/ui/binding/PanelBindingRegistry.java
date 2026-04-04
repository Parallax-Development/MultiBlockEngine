package dev.darkblade.mbe.api.ui.binding;

import org.bukkit.block.Block;

import java.util.Collection;
import java.util.Optional;

public interface PanelBindingRegistry {
    Optional<PanelBinding> getByBlock(Block block);
    Collection<PanelBinding> all();
}
