package dev.darkblade.mbe.platform.bukkit.preview.version;

import dev.darkblade.mbe.platform.bukkit.preview.api.BlockDisplayRenderer;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayBlockState;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayEntityHandle;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayTransform;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;

public final class VersionedRenderer implements BlockDisplayRenderer {
    private final VersionResolver versionResolver;
    private final Map<ProtocolVersion, BlockDisplayRenderer> renderers;
    private final BlockDisplayRenderer fallbackRenderer;

    public VersionedRenderer(
        VersionResolver versionResolver,
        Map<ProtocolVersion, BlockDisplayRenderer> renderers,
        BlockDisplayRenderer fallbackRenderer
    ) {
        this.versionResolver = Objects.requireNonNull(versionResolver, "versionResolver");
        this.renderers = Map.copyOf(Objects.requireNonNull(renderers, "renderers"));
        this.fallbackRenderer = Objects.requireNonNull(fallbackRenderer, "fallbackRenderer");
    }

    @Override
    public DisplayEntityHandle spawn(Player player, DisplaySpawnRequest request) {
        return resolve(player).spawn(player, request);
    }

    @Override
    public void updateTransform(Player player, DisplayEntityHandle handle, DisplayTransform transform) {
        resolve(player).updateTransform(player, handle, transform);
    }

    @Override
    public void updateBlock(Player player, DisplayEntityHandle handle, DisplayBlockState blockState) {
        resolve(player).updateBlock(player, handle, blockState);
    }

    @Override
    public void destroy(Player player, DisplayEntityHandle handle) {
        resolve(player).destroy(player, handle);
    }

    private BlockDisplayRenderer resolve(Player player) {
        ProtocolVersion version = versionResolver.resolve(player);
        BlockDisplayRenderer renderer = renderers.get(version);
        if (renderer != null) {
            return renderer;
        }
        BlockDisplayRenderer latest = renderers.get(ProtocolVersion.V1_21_11);
        if (latest != null) {
            return latest;
        }
        return fallbackRenderer;
    }
}
