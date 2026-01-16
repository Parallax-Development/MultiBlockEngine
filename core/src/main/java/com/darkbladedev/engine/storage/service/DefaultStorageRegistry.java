package com.darkbladedev.engine.storage.service;

import com.darkbladedev.engine.api.logging.EngineLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.storage.StorageDescriptor;
import com.darkbladedev.engine.api.storage.StorageExceptionHandler;
import com.darkbladedev.engine.api.storage.StorageListener;
import com.darkbladedev.engine.api.storage.StorageRegistry;
import com.darkbladedev.engine.api.storage.StorageResult;
import com.darkbladedev.engine.api.storage.StorageService;
import com.darkbladedev.engine.api.storage.StorageServiceFactory;
import com.darkbladedev.engine.api.storage.StorageSnapshot;
import com.darkbladedev.engine.api.storage.item.ItemKey;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultStorageRegistry implements StorageRegistry {

    private final EngineLogger logger;
    private final StorageExceptionHandler exceptionHandler;
    private final Map<String, StorageServiceFactory> factories = new ConcurrentHashMap<>();

    public DefaultStorageRegistry(EngineLogger logger, StorageExceptionHandler exceptionHandler) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
    }

    @Override
    public void registerFactory(String type, StorageServiceFactory factory) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");

        String key = normalizeType(type);
        StorageServiceFactory existing = factories.putIfAbsent(key, factory);
        if (existing != null && existing != factory) {
            logger.warn(
                    "Storage factory already registered",
                    LogKv.kv("type", key),
                    LogKv.kv("existing", existing.getClass().getName()),
                    LogKv.kv("attempt", factory.getClass().getName())
            );
        }
    }

    @Override
    public StorageService create(String type, StorageDescriptor descriptor) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(descriptor, "descriptor");

        String key = normalizeType(type);
        StorageServiceFactory factory = factories.get(key);
        if (factory == null) {
            logger.error("Storage factory not found", LogKv.kv("type", key));
            return new DisabledStorageService(descriptor.id(), "factory_not_found");
        }

        try {
            StorageService storage = factory.create(descriptor);
            if (storage == null) {
                logger.error("Storage factory returned null", LogKv.kv("type", key));
                return new DisabledStorageService(descriptor.id(), "factory_returned_null");
            }
            return new SafeStorageService(descriptor.id(), storage, exceptionHandler);
        } catch (Throwable t) {
            StorageService disabled = new DisabledStorageService(descriptor.id(), "factory_throw");
            exceptionHandler.handle(disabled, t);
            return disabled;
        }
    }

    private static String normalizeType(String type) {
        String s = type.trim();
        if (s.isEmpty()) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private static final class SafeStorageService implements StorageService {
        private final UUID id;
        private final AtomicReference<StorageService> delegate;
        private final StorageExceptionHandler handler;
        private final AtomicBoolean tripped = new AtomicBoolean(false);

        private SafeStorageService(UUID id, StorageService delegate, StorageExceptionHandler handler) {
            this.id = Objects.requireNonNull(id, "id");
            this.delegate = new AtomicReference<>(Objects.requireNonNull(delegate, "delegate"));
            this.handler = Objects.requireNonNull(handler, "handler");
        }

        @Override
        public long getAmount(ItemKey key) {
            try {
                return delegate.get().getAmount(key);
            } catch (Throwable t) {
                trip(t);
                return 0L;
            }
        }

        @Override
        public boolean contains(ItemKey key) {
            try {
                return delegate.get().contains(key);
            } catch (Throwable t) {
                trip(t);
                return false;
            }
        }

        @Override
        public Map<ItemKey, Long> getAll() {
            try {
                Map<ItemKey, Long> map = delegate.get().getAll();
                if (map == null || map.isEmpty()) {
                    return Map.of();
                }
                return Map.copyOf(map);
            } catch (Throwable t) {
                trip(t);
                return Map.of();
            }
        }

        @Override
        public long getTotalItems() {
            try {
                return delegate.get().getTotalItems();
            } catch (Throwable t) {
                trip(t);
                return 0L;
            }
        }

        @Override
        public long getDistinctItemCount() {
            try {
                return delegate.get().getDistinctItemCount();
            } catch (Throwable t) {
                trip(t);
                return 0L;
            }
        }

        @Override
        public StorageResult insert(ItemKey key, long amount) {
            try {
                return delegate.get().insert(key, amount);
            } catch (Throwable t) {
                trip(t);
                return StorageResult.ERROR;
            }
        }

        @Override
        public StorageResult extract(ItemKey key, long amount) {
            try {
                return delegate.get().extract(key, amount);
            } catch (Throwable t) {
                trip(t);
                return StorageResult.ERROR;
            }
        }

        @Override
        public boolean canInsert(ItemKey key, long amount) {
            try {
                return delegate.get().canInsert(key, amount);
            } catch (Throwable t) {
                trip(t);
                return false;
            }
        }

        @Override
        public boolean canExtract(ItemKey key, long amount) {
            try {
                return delegate.get().canExtract(key, amount);
            } catch (Throwable t) {
                trip(t);
                return false;
            }
        }

        @Override
        public void clear() {
            try {
                delegate.get().clear();
            } catch (Throwable t) {
                trip(t);
            }
        }

        @Override
        public StorageSnapshot snapshot() {
            try {
                return delegate.get().snapshot();
            } catch (Throwable t) {
                trip(t);
                return new BasicSnapshot(System.currentTimeMillis(), Map.of());
            }
        }

        @Override
        public void addListener(StorageListener listener) {
            try {
                delegate.get().addListener(listener);
            } catch (Throwable t) {
                trip(t);
            }
        }

        @Override
        public void removeListener(StorageListener listener) {
            try {
                delegate.get().removeListener(listener);
            } catch (Throwable t) {
                trip(t);
            }
        }

        private void trip(Throwable t) {
            if (!tripped.compareAndSet(false, true)) {
                return;
            }
            StorageService disabled = new DisabledStorageService(id, "exception");
            delegate.set(disabled);
            handler.handle(this, t);
        }
    }

    private static final class DisabledStorageService implements StorageService {
        private final UUID id;
        private final String reason;
        private final Set<StorageListener> listeners = new CopyOnWriteArraySet<>();

        private DisabledStorageService(UUID id, String reason) {
            this.id = Objects.requireNonNull(id, "id");
            this.reason = reason == null ? "disabled" : reason;
        }

        @Override
        public long getAmount(ItemKey key) {
            return 0L;
        }

        @Override
        public boolean contains(ItemKey key) {
            return false;
        }

        @Override
        public Map<ItemKey, Long> getAll() {
            return Map.of();
        }

        @Override
        public long getTotalItems() {
            return 0L;
        }

        @Override
        public long getDistinctItemCount() {
            return 0L;
        }

        @Override
        public StorageResult insert(ItemKey key, long amount) {
            return StorageResult.ERROR;
        }

        @Override
        public StorageResult extract(ItemKey key, long amount) {
            return StorageResult.ERROR;
        }

        @Override
        public boolean canInsert(ItemKey key, long amount) {
            return false;
        }

        @Override
        public boolean canExtract(ItemKey key, long amount) {
            return false;
        }

        @Override
        public void clear() {
            for (StorageListener l : listeners) {
                try {
                    l.onClear();
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public StorageSnapshot snapshot() {
            return new BasicSnapshot(System.currentTimeMillis(), Map.of());
        }

        @Override
        public void addListener(StorageListener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeListener(StorageListener listener) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        @Override
        public String toString() {
            return "DisabledStorageService{" +
                    "id=" + id +
                    ", reason='" + reason + "'" +
                    '}';
        }
    }

    private record BasicSnapshot(long timestamp, Map<ItemKey, Long> entries) implements StorageSnapshot {
        private BasicSnapshot {
            entries = entries == null || entries.isEmpty() ? Map.of() : Collections.unmodifiableMap(entries);
        }
    }
}
