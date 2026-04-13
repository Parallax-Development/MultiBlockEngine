package dev.darkblade.mbe.core.application.service.metadata;

import dev.darkblade.mbe.api.event.MetadataValueChangeEvent;
import dev.darkblade.mbe.api.metadata.ComputedMetadataKey;
import dev.darkblade.mbe.api.metadata.MetadataContext;
import dev.darkblade.mbe.api.metadata.MetadataKey;
import dev.darkblade.mbe.api.metadata.MetadataService;
import dev.darkblade.mbe.core.application.service.ManagedCoreService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class MetadataServiceImpl implements MetadataService, ManagedCoreService {
    private final Map<String, MetadataKey<?>> registry = new ConcurrentHashMap<>();
    private final Map<MultiblockInstance, MetadataContainer> byInstance = new ConcurrentHashMap<>();
    private final Map<PlaceholderCacheKey, CacheEntry> placeholderCache = new ConcurrentHashMap<>();
    private final long placeholderCacheTtlMs;

    public MetadataServiceImpl(long placeholderCacheTtlMs) {
        this.placeholderCacheTtlMs = Math.max(0L, placeholderCacheTtlMs);
    }

    @Override
    public String getServiceId() {
        return "mbe:metadata";
    }

    @Override
    public String getManagedCoreServiceId() {
        return getServiceId();
    }

    @Override
    public <T> void define(MetadataKey<T> key) {
        MetadataKey<T> safeKey = Objects.requireNonNull(key, "key");
        String id = normalizeId(safeKey.id());
        registry.put(id, safeKey);
        placeholderCache.clear();
    }

    @Override
    public <T> void set(MultiblockInstance instance, MetadataKey<T> key, T value) {
        MultiblockInstance safeInstance = Objects.requireNonNull(instance, "instance");
        MetadataKey<T> registered = requireRegistered(key);
        if (!registered.apiAccess().canWrite()) {
            throw new IllegalStateException("Metadata key is not writable by API: " + registered.id());
        }
        if (registered instanceof ComputedMetadataKey<?>) {
            throw new IllegalStateException("Computed metadata key cannot be stored: " + registered.id());
        }
        MetadataContainer existingContainer = byInstance.get(safeInstance);
        Object previousValue = existingContainer == null ? null : existingContainer.values().get(registered);
        if (value == null) {
            if (existingContainer != null) {
                existingContainer.values().remove(registered);
            }
            invalidateInstanceKey(safeInstance, registered.id());
            emitMetadataChange(safeInstance, registered.id(), previousValue, null);
            return;
        }
        if (!registered.type().isInstance(value)) {
            throw new IllegalArgumentException("Invalid metadata value type for key " + registered.id());
        }
        MetadataContainer container = byInstance.computeIfAbsent(safeInstance, ignored -> new MetadataContainer());
        container.values().put(registered, value);
        invalidateInstanceKey(safeInstance, registered.id());
        emitMetadataChange(safeInstance, registered.id(), previousValue, value);
    }

    @Override
    public <T> @Nullable T get(MultiblockInstance instance, MetadataKey<T> key) {
        MultiblockInstance safeInstance = Objects.requireNonNull(instance, "instance");
        MetadataKey<T> registered = requireRegistered(key);
        if (!registered.apiAccess().canRead()) {
            return null;
        }
        return castValue(registered, resolveValue(registered, new DefaultMetadataContext(safeInstance, null)));
    }

    @Override
    public @Nullable String resolveForPlaceholder(String id, MetadataContext context) {
        if (id == null || id.isBlank() || context == null || context.instance() == null) {
            return null;
        }
        String normalized = normalizeId(id);
        MetadataKey<?> key = registry.get(normalized);
        if (key == null || !key.papiAccess().canRead()) {
            return null;
        }
        if (!safeVisible(key, context)) {
            return null;
        }
        if (placeholderCacheTtlMs > 0L) {
            PlaceholderCacheKey cacheKey = new PlaceholderCacheKey(normalized, context.instance(), context.player() == null ? null : context.player().getUniqueId());
            String cached = readFromCache(cacheKey);
            if (cached != null) {
                return cached;
            }
            String resolved = formatForPlaceholder(key, context);
            if (resolved != null) {
                placeholderCache.put(cacheKey, new CacheEntry(resolved, System.currentTimeMillis()));
            }
            return resolved;
        }
        return formatForPlaceholder(key, context);
    }

    public void invalidateInstance(MultiblockInstance instance) {
        if (instance == null) {
            return;
        }
        byInstance.remove(instance);
        placeholderCache.keySet().removeIf(key -> key.instance() == instance);
    }

    public void invalidateInstanceKey(MultiblockInstance instance, String keyId) {
        if (instance == null || keyId == null || keyId.isBlank()) {
            return;
        }
        invalidatePlaceholderCacheForInstance(instance, keyId);
    }

    public void invalidateAll() {
        byInstance.clear();
        placeholderCache.clear();
    }

    @Override
    public void onCoreDisable() {
        invalidateAll();
    }

    private String formatForPlaceholder(MetadataKey<?> key, MetadataContext context) {
        Object value = resolveValueRaw(key, context);
        if (value == null) {
            return null;
        }
        return applyFormatter(key, value);
    }

    private Object resolveValueRaw(MetadataKey<?> key, MetadataContext context) {
        if (key instanceof ComputedMetadataKey<?> computed) {
            return computed.compute(context);
        }
        MetadataContainer container = byInstance.get(context.instance());
        if (container == null) {
            return null;
        }
        return container.values().get(key);
    }

    private <T> @Nullable T resolveValue(MetadataKey<T> key, MetadataContext context) {
        return castValue(key, resolveValueRaw(key, context));
    }

    private <T> @Nullable T castValue(MetadataKey<T> key, Object raw) {
        if (raw == null) {
            return null;
        }
        if (!key.type().isInstance(raw)) {
            return null;
        }
        return key.type().cast(raw);
    }

    private String applyFormatter(MetadataKey<?> key, Object value) {
        @SuppressWarnings("unchecked")
        MetadataKey<Object> casted = (MetadataKey<Object>) key;
        Function<Object, String> formatter = casted.formatter();
        String out = formatter.apply(value);
        if (out != null) {
            return out;
        }
        return String.valueOf(value);
    }

    private <T> MetadataKey<T> requireRegistered(MetadataKey<T> key) {
        MetadataKey<T> safeKey = Objects.requireNonNull(key, "key");
        String id = normalizeId(safeKey.id());
        MetadataKey<?> registered = registry.get(id);
        if (registered == null) {
            throw new IllegalStateException("Metadata key is not registered: " + id);
        }
        if (!registered.type().equals(safeKey.type())) {
            throw new IllegalStateException("Metadata key type mismatch: " + id);
        }
        @SuppressWarnings("unchecked")
        MetadataKey<T> casted = (MetadataKey<T>) registered;
        return casted;
    }

    private boolean safeVisible(MetadataKey<?> key, MetadataContext context) {
        try {
            return key.visibility().test(context);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void invalidatePlaceholderCacheForInstance(MultiblockInstance instance, String keyId) {
        String normalized = normalizeId(keyId);
        placeholderCache.keySet().removeIf(entry -> entry.instance() == instance && entry.keyId().equals(normalized));
    }

    private void emitMetadataChange(MultiblockInstance instance, String keyId, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        if (Bukkit.getServer() == null) {
            return;
        }
        Bukkit.getPluginManager().callEvent(new MetadataValueChangeEvent(instance, normalizeId(keyId), oldValue, newValue));
    }

    private @Nullable String readFromCache(PlaceholderCacheKey key) {
        CacheEntry entry = placeholderCache.get(key);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - entry.createdAtMs() > placeholderCacheTtlMs) {
            placeholderCache.remove(key);
            return null;
        }
        return entry.value();
    }

    private String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Metadata key id cannot be blank");
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("mbe_")) {
            return normalized;
        }
        return "mbe_" + normalized;
    }

    private record PlaceholderCacheKey(String keyId, MultiblockInstance instance, UUID playerId) {
    }

    private record CacheEntry(String value, long createdAtMs) {
    }
}
