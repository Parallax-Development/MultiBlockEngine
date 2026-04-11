package dev.darkblade.mbe.api.metadata;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface MetadataContext {

    MultiblockInstance instance();

    @Nullable
    Player player();
}
