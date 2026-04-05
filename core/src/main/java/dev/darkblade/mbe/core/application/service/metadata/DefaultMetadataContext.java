package dev.darkblade.mbe.core.application.service.metadata;

import dev.darkblade.mbe.api.metadata.MetadataContext;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record DefaultMetadataContext(MultiblockInstance instance, @Nullable Player player) implements MetadataContext {
    public DefaultMetadataContext {
        Objects.requireNonNull(instance, "instance");
    }
}
