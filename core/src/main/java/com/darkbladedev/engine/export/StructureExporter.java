package com.darkbladedev.engine.export;

import com.darkbladedev.engine.api.export.ExportBlockPos;
import com.darkbladedev.engine.api.export.ExportBlockSnapshot;
import com.darkbladedev.engine.api.export.ExportHook;
import com.darkbladedev.engine.api.export.ExportHookRegistry;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StructureExporter {

    public record ExportResult(String id, String yaml, int blocks, boolean hasController, List<String> warnings) {
    }

    public static final class ExportException extends RuntimeException {
        public ExportException(String message) {
            super(message);
        }
    }

    private final CoreLogger log;
    private final ExportHookRegistry hooks;
    private final ExportConfig config;

    public StructureExporter(CoreLogger log, ExportHookRegistry hooks, ExportConfig config) {
        this.log = Objects.requireNonNull(log, "log");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.config = Objects.requireNonNull(config, "config");
    }

    public ExportResult exportToFile(String id, ExportSession session, Path exportsDir) {
        ExportResult result = exportToYaml(id, session);
        String yaml = result.yaml;

        if (exportsDir == null) {
            throw new ExportException("Ruta de exports inválida");
        }
        try {
            Files.createDirectories(exportsDir);
        } catch (IOException e) {
            throw new ExportException("No se pudo crear el directorio de exports: " + exportsDir);
        }

        Path out = exportsDir.resolve(result.id + ".yml");
        if (Files.exists(out)) {
            throw new ExportException("El archivo ya existe: " + out.getFileName());
        }

        try {
            Files.writeString(out, yaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el archivo: " + out.getFileName());
        }
        return result;
    }

    public ExportResult exportToYaml(String id, ExportSession session) {
        String cleanId = validateId(id);

        Selection selection = buildSelection(session);
        BlockKey controllerKey = requireController(session, selection.world());

        DefaultExportContext ctx = new DefaultExportContext();

        Map<BlockKey, ExportBlockSnapshot> snapshots = snapshotSelection(selection, ctx);
        applySessionMarks(session, snapshots, ctx);
        applyHooks(snapshots, ctx);

        ExportBlockPos controllerPos = new ExportBlockPos(controllerKey.worldId(), controllerKey.x(), controllerKey.y(), controllerKey.z());
        String controllerMatch = toMatcherString(snapshots.get(controllerKey));
        if (controllerMatch == null || controllerMatch.isBlank()) {
            throw new ExportException("No se pudo representar el bloque controller");
        }

        String yaml = YamlStructureWriter.write(cleanId, controllerPos, controllerMatch, snapshots, ctx);

        log.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.INFO, "Export completed", null, new LogKv[] {
                LogKv.kv("id", cleanId),
                LogKv.kv("blocks", snapshots.size()),
                LogKv.kv("controller", "yes"),
                LogKv.kv("addons", hooks.hooks().isEmpty() ? "none" : "hooks")
        }, Set.of("export"));

        return new ExportResult(cleanId, yaml, snapshots.size(), true, ctx.warnings());
    }

    private static String validateId(String raw) {
        if (raw == null) {
            throw new ExportException("Falta id");
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            throw new ExportException("Falta id");
        }
        if (!t.matches("[a-z0-9_]+")) {
            throw new ExportException("Id inválido (usa snake_case): " + raw);
        }
        return t;
    }

    private Selection buildSelection(ExportSession session) {
        if (session == null) {
            throw new ExportException("No hay sesión de export");
        }
        Location a = session.pos1();
        Location b = session.pos2();
        if (a == null || b == null) {
            throw new ExportException("Selección incompleta (define pos1 y pos2)");
        }
        World wa = a.getWorld();
        World wb = b.getWorld();
        if (wa == null || wb == null || !Objects.equals(wa.getUID(), wb.getUID())) {
            throw new ExportException("pos1 y pos2 deben estar en el mismo mundo");
        }

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        ensureChunksLoaded(wa, minX, minZ, maxX, maxZ);

        return new Selection(wa, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void ensureChunksLoaded(World world, int minX, int minZ, int maxX, int maxZ) {
        int minCx = Math.floorDiv(minX, 16);
        int maxCx = Math.floorDiv(maxX, 16);
        int minCz = Math.floorDiv(minZ, 16);
        int maxCz = Math.floorDiv(maxZ, 16);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    throw new ExportException("La selección incluye chunks no cargados");
                }
            }
        }
    }

    private BlockKey requireController(ExportSession session, World world) {
        Optional<BlockKey> controllerOpt = session.controller();
        if (controllerOpt.isEmpty()) {
            throw new ExportException("Falta controller (usa /mbe export mark controller)");
        }
        BlockKey c = controllerOpt.get();
        if (world == null || !Objects.equals(world.getUID(), c.worldId())) {
            throw new ExportException("El controller no está dentro de la selección");
        }
        Selection sel = buildSelection(session);
        if (c.x() < sel.minX() || c.x() > sel.maxX() || c.y() < sel.minY() || c.y() > sel.maxY() || c.z() < sel.minZ() || c.z() > sel.maxZ()) {
            throw new ExportException("El controller no está dentro de la selección");
        }
        return c;
    }

    private Map<BlockKey, ExportBlockSnapshot> snapshotSelection(Selection sel, DefaultExportContext ctx) {
        Map<BlockKey, ExportBlockSnapshot> out = new LinkedHashMap<>();
        World w = sel.world();
        UUID wid = w.getUID();

        Set<Material> ignored = config.ignoredMaterials();

        for (int y = sel.minY(); y <= sel.maxY(); y++) {
            for (int x = sel.minX(); x <= sel.maxX(); x++) {
                for (int z = sel.minZ(); z <= sel.maxZ(); z++) {
                    Block block = w.getBlockAt(x, y, z);
                    Material mat = block.getType();

                    if (!config.includeAir() && mat.isAir()) {
                        continue;
                    }

                    if (!mat.isAir() && ignored.contains(mat)) {
                        ctx.warn("Block ignorado: " + mat.name());
                        continue;
                    }

                    ExportBlockPos pos = new ExportBlockPos(wid, x, y, z);
                    String blockData = config.includeBlockStates() ? blockDataString(block, config.includeWaterlogged()) : null;
                    out.put(new BlockKey(wid, x, y, z), new ExportBlockSnapshot(pos, mat, blockData));
                }
            }
        }

        if (out.isEmpty()) {
            throw new ExportException("La selección está vacía");
        }

        return out;
    }

    private static String blockDataString(Block block, boolean includeWaterlogged) {
        if (block == null) {
            return null;
        }
        BlockData bd = block.getBlockData();
        if (!includeWaterlogged && bd instanceof Waterlogged wl) {
            if (wl.isWaterlogged()) {
                BlockData clone = bd.clone();
                if (clone instanceof Waterlogged wl2) {
                    wl2.setWaterlogged(false);
                    bd = clone;
                }
            }
        }

        String as = bd.getAsString(true);
        return as == null ? null : as.trim();
    }

    private static void applySessionMarks(ExportSession session, Map<BlockKey, ExportBlockSnapshot> snapshots, DefaultExportContext ctx) {
        Map<BlockKey, String> roles = session.rolesSnapshot();
        for (Map.Entry<BlockKey, String> e : roles.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            ExportBlockSnapshot snap = snapshots.get(e.getKey());
            if (snap == null) {
                continue;
            }
            ctx.markRole(snap.pos(), e.getValue());
        }

        Map<BlockKey, Map<String, Object>> props = session.propsSnapshot();
        for (Map.Entry<BlockKey, Map<String, Object>> e : props.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            ExportBlockSnapshot snap = snapshots.get(e.getKey());
            if (snap == null) {
                continue;
            }
            for (Map.Entry<String, Object> kv : e.getValue().entrySet()) {
                if (kv.getKey() == null || kv.getKey().isBlank()) {
                    continue;
                }
                ctx.putProperty(snap.pos(), kv.getKey(), kv.getValue());
            }
        }
    }

    private void applyHooks(Map<BlockKey, ExportBlockSnapshot> snapshots, DefaultExportContext ctx) {
        List<ExportHook> hs = hooks.hooks();
        if (hs.isEmpty()) {
            return;
        }

        for (ExportBlockSnapshot snap : snapshots.values()) {
            if (snap == null) {
                continue;
            }
            for (ExportHook h : hs) {
                if (h == null) {
                    continue;
                }
                try {
                    h.onBlockExport(snap, ctx);
                } catch (Throwable t) {
                    ctx.warn("Hook falló: " + t.getClass().getSimpleName());
                }
            }
        }
    }

    private static String toMatcherString(ExportBlockSnapshot snap) {
        if (snap == null) {
            return null;
        }
        if (snap.blockData() != null && snap.blockData().contains("[")) {
            return snap.blockData();
        }
        Material mat = snap.material();
        return mat == null ? null : mat.name();
    }
}
