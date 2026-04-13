package dev.darkblade.mbe.core.application.service.query;

import dev.darkblade.mbe.core.application.service.ManagedCoreService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMultiblockQueryServiceImpl implements PlayerMultiblockQueryService, ManagedCoreService {
    private static final Set<String> OWNER_KEYS = Set.of(
            "owner",
            "ownerId",
            "owner_id",
            "ownerUuid",
            "owner_uuid",
            "playerId",
            "player_id",
            "playerUuid",
            "player_uuid",
            "creator",
            "creatorId",
            "creator_id",
            "creatorUuid",
            "creator_uuid",
            "createdBy"
    );

    private final MultiblockRuntimeService runtimeService;
    private final long cacheTtlMs;
    private final Map<InstancesCacheKey, CacheEntry<List<MultiblockInstance>>> instancesCache = new ConcurrentHashMap<>();
    private final Map<ValuesCacheKey, CacheEntry<List<Object>>> valuesCache = new ConcurrentHashMap<>();
    private final Map<AggregateCacheKey, CacheEntry<Double>> aggregateCache = new ConcurrentHashMap<>();
    private final Map<AnchorKey, UUID> ownerByAnchor = new ConcurrentHashMap<>();

    public PlayerMultiblockQueryServiceImpl(MultiblockRuntimeService runtimeService, long cacheTtlMs) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
        this.cacheTtlMs = Math.max(0L, cacheTtlMs);
    }

    @Override
    public List<MultiblockInstance> getPlayerInstances(UUID playerId, String multiblockId) {
        if (playerId == null) {
            return List.of();
        }
        String normalizedMultiblockId = normalizeMultiblockId(multiblockId);
        InstancesCacheKey key = new InstancesCacheKey(playerId, normalizedMultiblockId);
        return getCached(instancesCache, key, this::buildPlayerInstances);
    }

    @Override
    public List<Object> getVariableValues(UUID playerId, String multiblockId, String varName) {
        if (playerId == null || varName == null || varName.isBlank()) {
            return List.of();
        }
        String normalizedMultiblockId = normalizeMultiblockId(multiblockId);
        String normalizedVarName = normalizeVarName(varName);
        ValuesCacheKey key = new ValuesCacheKey(playerId, normalizedMultiblockId, normalizedVarName);
        return getCached(valuesCache, key, this::buildVariableValues);
    }

    @Override
    public double aggregate(UUID playerId, String multiblockId, String varName, AggregationType type) {
        if (playerId == null || type == null) {
            return 0D;
        }
        String normalizedMultiblockId = normalizeMultiblockId(multiblockId);
        String normalizedVarName = normalizeVarName(varName);
        AggregateCacheKey key = new AggregateCacheKey(playerId, normalizedMultiblockId, normalizedVarName, type);
        return getCached(aggregateCache, key, this::buildAggregate);
    }

    @Override
    public int countInstances(UUID playerId, String multiblockId) {
        return getPlayerInstances(playerId, multiblockId).size();
    }

    @Override
    public String getManagedCoreServiceId() {
        return "mbe:player-query";
    }

    public void trackOwnership(UUID playerId, MultiblockInstance instance) {
        if (playerId == null || instance == null) {
            return;
        }
        AnchorKey anchorKey = toAnchorKey(instance.anchorLocation());
        if (anchorKey == null) {
            return;
        }
        ownerByAnchor.put(anchorKey, playerId);
        invalidatePlayer(playerId);
    }

    public void removeOwnership(MultiblockInstance instance) {
        if (instance == null) {
            return;
        }
        AnchorKey anchorKey = toAnchorKey(instance.anchorLocation());
        if (anchorKey == null) {
            invalidateAll();
            return;
        }
        UUID owner = ownerByAnchor.remove(anchorKey);
        if (owner != null) {
            invalidatePlayer(owner);
            return;
        }
        invalidateAll();
    }

    public void invalidatePlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        instancesCache.keySet().removeIf(key -> playerId.equals(key.playerId()));
        valuesCache.keySet().removeIf(key -> playerId.equals(key.playerId()));
        aggregateCache.keySet().removeIf(key -> playerId.equals(key.playerId()));
    }

    public void invalidateAll() {
        instancesCache.clear();
        valuesCache.clear();
        aggregateCache.clear();
    }

    @Override
    public void onCoreDisable() {
        invalidateAll();
    }

    public int getAggregateCacheSize() {
        return aggregateCache.size();
    }

    private List<MultiblockInstance> buildPlayerInstances(InstancesCacheKey key) {
        Collection<MultiblockInstance> activeInstances = runtimeService.getActiveInstancesSnapshot();
        if (activeInstances.isEmpty()) {
            return List.of();
        }
        List<MultiblockInstance> out = new ArrayList<>();
        for (MultiblockInstance instance : activeInstances) {
            if (instance == null || instance.type() == null || instance.type().id() == null) {
                continue;
            }
            if (!matchesMultiblock(instance.type().id(), key.multiblockId())) {
                continue;
            }
            if (!belongsToPlayer(instance, key.playerId())) {
                continue;
            }
            out.add(instance);
        }
        return List.copyOf(out);
    }

    private List<Object> buildVariableValues(ValuesCacheKey key) {
        List<MultiblockInstance> instances = getPlayerInstances(key.playerId(), key.multiblockId());
        if (instances.isEmpty()) {
            return List.of();
        }
        List<Object> values = new ArrayList<>();
        for (MultiblockInstance instance : instances) {
            Object value = instance.getVariable(key.varName());
            if (value != null) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private double buildAggregate(AggregateCacheKey key) {
        if (key.aggregationType() == AggregationType.COUNT) {
            return countInstances(key.playerId(), key.multiblockId());
        }
        List<Object> values = getVariableValues(key.playerId(), key.multiblockId(), key.varName());
        if (values.isEmpty()) {
            return 0D;
        }
        List<Double> numericValues = values.stream()
                .map(this::toDouble)
                .filter(Objects::nonNull)
                .toList();
        if (numericValues.isEmpty()) {
            return 0D;
        }
        return switch (key.aggregationType()) {
            case SUM -> numericValues.stream().mapToDouble(Double::doubleValue).sum();
            case AVG -> numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
            case MIN -> numericValues.stream().min(Comparator.naturalOrder()).orElse(0D);
            case MAX -> numericValues.stream().max(Comparator.naturalOrder()).orElse(0D);
            case COUNT -> countInstances(key.playerId(), key.multiblockId());
        };
    }

    private boolean matchesMultiblock(String instanceTypeId, String expectedTypeId) {
        if (expectedTypeId == null || expectedTypeId.isBlank()) {
            return true;
        }
        return expectedTypeId.equalsIgnoreCase(instanceTypeId);
    }

    private boolean belongsToPlayer(MultiblockInstance instance, UUID playerId) {
        AnchorKey anchorKey = toAnchorKey(instance.anchorLocation());
        if (anchorKey != null) {
            UUID owner = ownerByAnchor.get(anchorKey);
            if (playerId.equals(owner)) {
                return true;
            }
        }
        for (String key : OWNER_KEYS) {
            Object value = instance.getVariable(key);
            UUID variableOwner = toUuid(value);
            if (playerId.equals(variableOwner)) {
                if (anchorKey != null) {
                    ownerByAnchor.put(anchorKey, playerId);
                }
                return true;
            }
        }
        return false;
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String raw && !raw.isBlank()) {
            try {
                return UUID.fromString(raw.trim());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeMultiblockId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeVarName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String raw && !raw.isBlank()) {
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private <K, V> V getCached(Map<K, CacheEntry<V>> cache, K key, java.util.function.Function<K, V> provider) {
        long now = System.currentTimeMillis();
        CacheEntry<V> current = cache.get(key);
        if (current != null && !current.isExpired(now, cacheTtlMs)) {
            return current.value();
        }
        V value = provider.apply(key);
        cache.put(key, new CacheEntry<>(value, now));
        return value;
    }

    private AnchorKey toAnchorKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new AnchorKey(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    private record InstancesCacheKey(UUID playerId, String multiblockId) {
    }

    private record ValuesCacheKey(UUID playerId, String multiblockId, String varName) {
    }

    private record AggregateCacheKey(UUID playerId, String multiblockId, String varName, AggregationType aggregationType) {
    }

    private record CacheEntry<T>(T value, long createdAtMs) {
        private boolean isExpired(long now, long ttlMs) {
            return ttlMs <= 0L || now - createdAtMs > ttlMs;
        }
    }

    private record AnchorKey(String world, int x, int y, int z) {
    }
}
