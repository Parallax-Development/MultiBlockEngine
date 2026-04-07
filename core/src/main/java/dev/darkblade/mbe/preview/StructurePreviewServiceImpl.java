package dev.darkblade.mbe.preview;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StructurePreviewServiceImpl implements StructurePreviewService {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_PREVIEW_STARTED = MessageKey.of(ORIGIN, "core.preview.started");
    private static final MessageKey MSG_PREVIEW_CANCELLED = MessageKey.of(ORIGIN, "core.preview.cancelled");
    private static final MessageKey MSG_PREVIEW_COMPLETED = MessageKey.of(ORIGIN, "core.preview.completed");

    private final JavaPlugin plugin;
    private final DisplayEntityRenderer renderer;
    private final I18nService i18n;
    private final PreviewSessionManager sessions;
    private final PreviewValidationStrategy validationStrategy;
    private final Queue<RenderTask> renderQueue;
    private final int batchSize;
    private final int raycastDistance;
    private final double maxDistanceSquared;
    private final Duration timeout;
    private BukkitTask tickTask;

    public StructurePreviewServiceImpl(
        JavaPlugin plugin,
        DisplayEntityRenderer renderer,
        I18nService i18n,
        PreviewValidationStrategy validationStrategy,
        PreviewSettings settings
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.i18n = i18n;
        this.validationStrategy = Objects.requireNonNull(validationStrategy, "validationStrategy");
        this.sessions = new PreviewSessionManager();
        this.renderQueue = new ConcurrentLinkedQueue<>();
        this.batchSize = Math.max(1, settings.batchSize());
        this.raycastDistance = Math.max(2, settings.raycastDistance());
        double maxDistance = Math.max(2.0D, settings.maxDistance());
        this.maxDistanceSquared = maxDistance * maxDistance;
        this.timeout = settings.timeout() == null ? Duration.ofSeconds(20) : settings.timeout();
    }

    public void start() {
        if (tickTask != null) {
            return;
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (PreviewSession session : new ArrayList<>(sessions.all())) {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player != null) {
                destroyPreview(player);
            } else {
                sessions.remove(session.playerId());
                session.clearBlocks();
            }
        }
        renderQueue.clear();
    }

    @Override
    public PreviewSession startPreview(Player player, MultiblockDefinition definition) {
        if (player == null || definition == null) {
            return null;
        }
        destroyPreview(player);
        Location origin = resolveInitialOrigin(player);
        PreviewSession session = new PreviewSession(player.getUniqueId(), definition, origin, Rotation.NORTH);
        session.state(PreviewState.MOVING);
        sessions.put(session);
        queueSpawn(player, session, session.currentRenderVersion());
        send(player, MSG_PREVIEW_STARTED);
        return session;
    }

    @Override
    public void updatePreviewOrigin(Player player, Location newOrigin) {
        if (player == null || newOrigin == null) {
            return;
        }
        PreviewSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (newOrigin.getWorld() == null || !newOrigin.getWorld().equals(player.getWorld())) {
            destroyPreview(player);
            return;
        }
        if (session.state() == PreviewState.LOCKED) {
            session.touch();
            return;
        }
        Location snapped = newOrigin.getBlock().getLocation();
        if (sameBlock(session.origin(), snapped)) {
            session.touch();
            return;
        }
        session.origin(snapped);
        rerender(player, session);
    }

    @Override
    public void rotatePreview(Player player, Rotation rotation) {
        if (player == null) {
            return;
        }
        PreviewSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.rotation(rotation == null ? Rotation.NORTH : rotation);
        rerender(player, session);
    }

    @Override
    public void destroyPreview(Player player) {
        destroyPreview(player, true);
    }

    private void destroyPreview(Player player, boolean notifyCancelled) {
        if (player == null) {
            return;
        }
        PreviewSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        List<Integer> ids = collectEntityIds(session);
        if (!ids.isEmpty()) {
            renderer.destroyEntities(player, ids);
        }
        session.clearBlocks();
        if (notifyCancelled) {
            send(player, MSG_PREVIEW_CANCELLED);
        }
    }

    @Override
    public boolean hasActivePreview(Player player) {
        return player != null && sessions.has(player.getUniqueId());
    }

    public void touch(Player player) {
        if (player == null) {
            return;
        }
        PreviewSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.touch();
        }
    }

    public PreviewSession getSession(Player player) {
        if (player == null) {
            return null;
        }
        return sessions.get(player.getUniqueId());
    }

    public boolean switchDefinition(Player player, MultiblockDefinition definition) {
        if (player == null || definition == null) {
            return false;
        }
        PreviewSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        session.definition(definition);
        rerender(player, session);
        return true;
    }

    void handlePlacedBlock(Player player, BlockPosition position, BlockData placedBlockData) {
        if (player == null || position == null || placedBlockData == null) {
            return;
        }
        PreviewSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        SessionPreviewBlock previewBlock = session.blocks().get(position);
        if (previewBlock == null || previewBlock.completed()) {
            return;
        }
        if (!matches(placedBlockData, previewBlock.expected())) {
            return;
        }
        if (!session.markCompleted(position)) {
            return;
        }
        renderer.destroyEntities(player, List.of(previewBlock.entityId()));
        session.touch();
        if (session.isCompleted()) {
            destroyPreview(player, false);
            send(player, MSG_PREVIEW_COMPLETED);
        }
    }

    private void rerender(Player player, PreviewSession session) {
        List<Integer> ids = collectEntityIds(session);
        if (!ids.isEmpty()) {
            renderer.destroyEntities(player, ids);
        }
        session.clearBlocks();
        queueSpawn(player, session, session.nextRenderVersion());
    }

    private void queueSpawn(Player player, PreviewSession session, long renderVersion) {
        if (session.definition() == null || session.definition().blocks() == null) {
            return;
        }
        for (PreviewBlock block : session.definition().blocks()) {
            if (block == null || block.localPosition() == null || block.blockData() == null) {
                continue;
            }
            Vector3i rotated = VectorUtils.rotate(block.localPosition(), session.rotation());
            Location worldLocation = rotated.addTo(session.origin());
            if (validationStrategy.validate(worldLocation, block.blockData()) == PreviewBlockState.INVALID) {
                continue;
            }
            BlockPosition position = new BlockPosition(
                worldLocation.getWorld(),
                worldLocation.getBlockX(),
                worldLocation.getBlockY(),
                worldLocation.getBlockZ()
            );
            renderQueue.offer(new RenderTask(player, session, renderVersion, position, worldLocation, block.blockData()));
        }
        session.touch();
    }

    private void tick() {
        processQueue();
        cleanupSessions();
    }

    private void processQueue() {
        int processed = 0;
        while (processed < batchSize) {
            RenderTask task = renderQueue.poll();
            if (task == null) {
                return;
            }
            Player player = task.player();
            if (player == null || !player.isOnline()) {
                continue;
            }
            PreviewSession active = sessions.get(player.getUniqueId());
            if (active == null || active != task.session()) {
                continue;
            }
            if (active.currentRenderVersion() != task.renderVersion()) {
                continue;
            }
            int entityId = renderer.spawnBlockDisplay(player, task.worldLocation(), task.blockData());
            if (entityId > 0) {
                SessionPreviewBlock previous = active.blocks().get(task.blockPosition());
                if (previous != null && previous.completed()) {
                    renderer.destroyEntities(player, List.of(entityId));
                } else {
                    if (previous != null && previous.entityId() > 0 && previous.entityId() != entityId) {
                        renderer.destroyEntities(player, List.of(previous.entityId()));
                    }
                    active.trackBlock(task.blockPosition(), new SessionPreviewBlock(task.blockData(), entityId, previous != null && previous.completed()));
                }
            }
            processed++;
        }
    }

    private void cleanupSessions() {
        Instant now = Instant.now();
        for (PreviewSession session : new ArrayList<>(sessions.all())) {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player == null || !player.isOnline()) {
                sessions.remove(session.playerId());
                session.clearBlocks();
                continue;
            }
            if (session.origin() == null || session.origin().getWorld() == null || !session.origin().getWorld().equals(player.getWorld())) {
                destroyPreview(player);
                continue;
            }
            if (Duration.between(session.lastTouchedAt(), now).compareTo(timeout) > 0) {
                destroyPreview(player);
                continue;
            }
            if (session.origin().distanceSquared(player.getLocation()) > maxDistanceSquared) {
                destroyPreview(player);
            }
        }
    }

    private Location resolveInitialOrigin(Player player) {
        Block target = player.getTargetBlockExact(raycastDistance);
        if (target != null && target.getWorld() != null) {
            BlockFace face = player.getTargetBlockFace(raycastDistance);
            Location origin = target.getLocation();
            if (face != null) {
                origin = origin.add(face.getModX(), face.getModY(), face.getModZ());
            }
            return origin.getBlock().getLocation();
        }
        Location eye = player.getEyeLocation();
        Location projected = eye.clone().add(eye.getDirection().normalize().multiply(3.0D));
        return projected.getBlock().getLocation();
    }

    private List<Integer> collectEntityIds(PreviewSession session) {
        List<Integer> ids = new ArrayList<>(session.blocks().size());
        for (SessionPreviewBlock block : session.blocks().values()) {
            if (block == null || block.entityId() <= 0) {
                continue;
            }
            ids.add(block.entityId());
        }
        return ids;
    }

    private boolean matches(BlockData placed, BlockData expected) {
        if (placed == null || expected == null) {
            return false;
        }
        if (placed.getMaterial() != expected.getMaterial()) {
            return false;
        }
        return placed.matches(expected) && expected.matches(placed);
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) {
            return false;
        }
        if (!Objects.equals(a.getWorld(), b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private void send(Player player, MessageKey key) {
        if (player == null || key == null || i18n == null) {
            return;
        }
        try {
            i18n.send(player, key);
        } catch (Throwable ignored) {
        }
    }
}
