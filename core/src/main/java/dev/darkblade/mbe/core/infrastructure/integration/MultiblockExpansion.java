package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MetricsService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.api.metadata.MetadataService;
import dev.darkblade.mbe.core.application.service.metadata.DefaultMetadataContext;
import dev.darkblade.mbe.core.application.service.metadata.PlayerMultiblockContextResolver;
import dev.darkblade.mbe.core.application.service.query.AggregationType;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class MultiblockExpansion extends PlaceholderExpansion {

    private final List<String> authors;
    private final String version;
    private final Supplier<MultiblockRuntimeService> runtimeServiceSupplier;
    private final PlayerMultiblockQueryService queryService;
    private final MetadataService metadataService;
    private final PlayerMultiblockContextResolver metadataContextResolver;
    private final int maxListSize;

    public MultiblockExpansion(
            MultiBlockEngine plugin,
            PlayerMultiblockQueryService queryService,
            MetadataService metadataService,
            PlayerMultiblockContextResolver metadataContextResolver,
            int maxListSize
    ) {
        this(
                plugin.getPluginMeta().getAuthors(),
                plugin.getPluginMeta().getVersion(),
                plugin::getManager,
                queryService,
                metadataService,
                metadataContextResolver,
                maxListSize
        );
    }

    MultiblockExpansion(
            List<String> authors,
            String version,
            Supplier<MultiblockRuntimeService> runtimeServiceSupplier,
            PlayerMultiblockQueryService queryService,
            MetadataService metadataService,
            PlayerMultiblockContextResolver metadataContextResolver,
            int maxListSize
    ) {
        this.authors = List.copyOf(authors);
        this.version = version;
        this.runtimeServiceSupplier = Objects.requireNonNull(runtimeServiceSupplier, "runtimeServiceSupplier");
        this.queryService = Objects.requireNonNull(queryService, "queryService");
        this.metadataService = Objects.requireNonNull(metadataService, "metadataService");
        this.metadataContextResolver = Objects.requireNonNull(metadataContextResolver, "metadataContextResolver");
        this.maxListSize = Math.max(1, maxListSize);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mbe";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }
    
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        MultiblockRuntimeService manager = runtimeServiceSupplier.get();
        MetricsService metrics = manager.getMetrics();

        if (params.equalsIgnoreCase("types_count")) {
            return String.valueOf(manager.getTypes().size());
        }

        if (params.equalsIgnoreCase("created_count")) {
            return String.valueOf(metrics.getCreatedInstances());
        }

        if (params.equalsIgnoreCase("destroyed_count")) {
            return String.valueOf(metrics.getDestroyedInstances());
        }

        if (params.equalsIgnoreCase("avg_tick_ms")) {
            return String.format("%.2f", metrics.getAverageTickTimeMs());
        }

        if (params.toLowerCase(Locale.ROOT).startsWith("multiblock_")) {
            return resolveMultiblockMetadataPlaceholder(player, params);
        }

        UUID playerId = player == null ? null : player.getUniqueId();
        if (playerId == null) {
            return null;
        }

        String[] parts = params.split("_");
        if (parts.length < 3 || !"player".equalsIgnoreCase(parts[0])) {
            return null;
        }

        String multiblockId = parts[1];
        String selector = parts[2].toLowerCase(Locale.ROOT);
        if ("amount".equals(selector)) {
            return String.valueOf(queryService.countInstances(playerId, multiblockId));
        }

        if (!"var".equals(selector) || parts.length < 5) {
            return null;
        }

        String varName = parts[3];
        String operation = parts[4].toLowerCase(Locale.ROOT);

        if ("list".equals(operation)) {
            return formatList(playerId, multiblockId, varName);
        }
        if ("percent".equals(operation)) {
            return formatPercent(playerId, multiblockId, varName);
        }

        AggregationType aggregationType = parseAggregation(operation);
        if (aggregationType == null) {
            return null;
        }
        if (aggregationType == AggregationType.COUNT) {
            List<Object> values = queryService.getVariableValues(playerId, multiblockId, varName);
            return String.valueOf(values.size());
        }
        double value = queryService.aggregate(playerId, multiblockId, varName, aggregationType);
        return String.format("%.2f", value);
    }

    private String formatList(UUID playerId, String multiblockId, String varName) {
        List<Object> values = queryService.getVariableValues(playerId, multiblockId, varName);
        if (values.isEmpty()) {
            return "";
        }
        List<String> sorted = values.stream()
                .map(this::normalizeValue)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(this::asSortableNumber).thenComparing(String::compareToIgnoreCase))
                .limit(maxListSize)
                .toList();
        return String.join(",", sorted);
    }

    private String formatPercent(UUID playerId, String multiblockId, String varName) {
        double sum = queryService.aggregate(playerId, multiblockId, varName, AggregationType.SUM);
        String[] maxCandidates = new String[] {
                "max_" + varName,
                varName + "_max",
                varName + "Max",
                "maxVar",
                "max"
        };
        for (String candidate : maxCandidates) {
            List<Object> candidateValues = queryService.getVariableValues(playerId, multiblockId, candidate);
            if (candidateValues.isEmpty()) {
                continue;
            }
            double maxSum = queryService.aggregate(playerId, multiblockId, candidate, AggregationType.SUM);
            if (maxSum <= 0D) {
                return "N/A";
            }
            double percent = (sum / maxSum) * 100D;
            return String.format("%.2f%%", percent);
        }
        return "N/A";
    }

    private AggregationType parseAggregation(String operation) {
        return switch (operation) {
            case "sum" -> AggregationType.SUM;
            case "avg" -> AggregationType.AVG;
            case "min" -> AggregationType.MIN;
            case "max" -> AggregationType.MAX;
            case "count" -> AggregationType.COUNT;
            default -> null;
        };
    }

    private String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            double numeric = number.doubleValue();
            if (numeric == Math.rint(numeric)) {
                return String.valueOf((long) numeric);
            }
            return String.format("%.2f", numeric);
        }
        return String.valueOf(value);
    }

    private double asSortableNumber(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return Double.MAX_VALUE;
        }
    }

    private @Nullable String resolveMultiblockMetadataPlaceholder(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return null;
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return null;
        }
        MultiblockInstance instance = metadataContextResolver.resolveNearest(player);
        if (instance == null) {
            return null;
        }
        return metadataService.resolveForPlaceholder(params, new DefaultMetadataContext(instance, player));
    }
}
