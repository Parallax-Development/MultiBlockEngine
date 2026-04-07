package dev.darkblade.mbe.core.application.service.limit;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class PermissionBasedLimitResolver implements MultiblockLimitResolver {
    private final File limitsFile;
    private final CoreLogger log;
    private final AtomicReference<List<MultiblockLimitDefinition>> definitions = new AtomicReference<>(List.of());

    public PermissionBasedLimitResolver(File limitsFile, CoreLogger log) {
        this.limitsFile = Objects.requireNonNull(limitsFile, "limitsFile");
        this.log = Objects.requireNonNull(log, "log");
        reload();
    }

    public void reload() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(limitsFile);
        List<Map<?, ?>> rows = yaml.getMapList("limits");
        if (rows == null || rows.isEmpty()) {
            definitions.set(List.of());
            return;
        }
        List<MultiblockLimitDefinition> parsed = new ArrayList<>();
        for (Map<?, ?> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            String permission = normalizePermission(row.get("permission"));
            if (permission.isBlank()) {
                continue;
            }
            int max = normalizeMax(row.get("max"));
            LimitScope scope = normalizeScope(row.get("scope"));
            String multiblockId = normalizeMultiblock(row.get("multiblock"));
            if (scope == LimitScope.PER_MULTIBLOCK && multiblockId.isBlank()) {
                continue;
            }
            parsed.add(new MultiblockLimitDefinition(permission, max, scope, multiblockId));
        }
        parsed.sort(Comparator.comparingInt(PermissionBasedLimitResolver::strictness));
        definitions.set(List.copyOf(parsed));
        log.debug("Limits loaded", LogKv.kv("count", parsed.size()), LogKv.kv("file", limitsFile.getName()));
    }

    @Override
    public Optional<MultiblockLimitDefinition> resolve(Player player, String multiblockId) {
        if (player == null) {
            return Optional.empty();
        }
        String normalizedId = normalizeMultiblock(multiblockId);
        List<MultiblockLimitDefinition> all = definitions.get();
        if (all.isEmpty()) {
            return Optional.empty();
        }

        List<MultiblockLimitDefinition> specific = new ArrayList<>();
        List<MultiblockLimitDefinition> global = new ArrayList<>();
        for (MultiblockLimitDefinition def : all) {
            if (def == null) {
                continue;
            }
            if (!player.hasPermission(def.permission())) {
                continue;
            }
            if (def.scope() == LimitScope.PER_MULTIBLOCK) {
                if (normalizedId.isBlank() || !normalizedId.equals(normalizeMultiblock(def.multiblockId()))) {
                    continue;
                }
                specific.add(def);
                continue;
            }
            global.add(def);
        }
        if (!specific.isEmpty()) {
            return specific.stream().min(Comparator.comparingInt(PermissionBasedLimitResolver::strictness));
        }
        return global.stream().min(Comparator.comparingInt(PermissionBasedLimitResolver::strictness));
    }

    private static int strictness(MultiblockLimitDefinition def) {
        if (def == null) {
            return Integer.MAX_VALUE;
        }
        if (def.max() == -1) {
            return Integer.MAX_VALUE;
        }
        return Math.max(def.max(), 0);
    }

    private static int normalizeMax(Object value) {
        if (value instanceof Number number) {
            int max = number.intValue();
            return max < -1 ? -1 : max;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value));
            return parsed < -1 ? -1 : parsed;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String normalizePermission(Object value) {
        String out = value == null ? "" : String.valueOf(value).trim();
        return out.toLowerCase(Locale.ROOT);
    }

    private static LimitScope normalizeScope(Object value) {
        String out = value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if ("PER_MULTIBLOCK".equals(out)) {
            return LimitScope.PER_MULTIBLOCK;
        }
        return LimitScope.GLOBAL;
    }

    private static String normalizeMultiblock(Object value) {
        String out = value == null ? "" : String.valueOf(value).trim();
        return out.toLowerCase(Locale.ROOT);
    }
}
