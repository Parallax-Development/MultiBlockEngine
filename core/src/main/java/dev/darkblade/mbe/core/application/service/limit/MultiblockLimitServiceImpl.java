package dev.darkblade.mbe.core.application.service.limit;

import dev.darkblade.mbe.api.persistence.PersistentStorageService;
import dev.darkblade.mbe.api.persistence.StorageRecordMeta;
import dev.darkblade.mbe.api.persistence.StorageSchema;
import dev.darkblade.mbe.api.persistence.StorageStore;
import dev.darkblade.mbe.api.persistence.StoredRecord;
import dev.darkblade.mbe.api.service.InjectService;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiblockLimitServiceImpl implements MultiblockLimitService {
    private static final String NAMESPACE = "core";
    private static final String DOMAIN = "limits";
    private static final String STORE_ID = "counters";

    @InjectService
    private PersistentStorageService injectedPersistence;
    @InjectService
    private MultiblockLimitResolver injectedResolver;

    private final PersistentStorageService persistence;
    private final MultiblockLimitResolver resolver;
    private final StorageStore store;
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    public MultiblockLimitServiceImpl(PersistentStorageService persistence, MultiblockLimitResolver resolver) {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.store = this.persistence.namespace(NAMESPACE).domain(DOMAIN).store(STORE_ID, schema());
    }

    @Override
    public String getServiceId() {
        return "mbe:limit.multiblock";
    }

    @Override
    public boolean canAssemble(Player player, String multiblockId) {
        if (player == null) {
            return true;
        }
        Optional<MultiblockLimitDefinition> defOpt = resolver.resolve(player, multiblockId);
        if (defOpt.isEmpty()) {
            return true;
        }
        return canAssemble(player.getUniqueId(), multiblockId, defOpt.get());
    }

    @Override
    public boolean canAssemble(UUID playerId, String multiblockId, MultiblockLimitDefinition definition) {
        if (playerId == null || definition == null) {
            return true;
        }
        if (definition.max() < 0) {
            return true;
        }
        String normalizedMultiblock = normalizeMultiblock(multiblockId);
        String key = definition.scope() == LimitScope.PER_MULTIBLOCK
                ? perMultiblockKey(playerId, normalizedMultiblock)
                : globalKey(playerId);
        int current = readCounter(key);
        return current < definition.max();
    }

    @Override
    public int getCurrentCount(Player player, String multiblockId) {
        if (player == null) {
            return 0;
        }
        Optional<MultiblockLimitDefinition> defOpt = resolver.resolve(player, multiblockId);
        if (defOpt.isEmpty()) {
            return 0;
        }
        MultiblockLimitDefinition definition = defOpt.get();
        String normalizedMultiblock = normalizeMultiblock(multiblockId);
        String key = definition.scope() == LimitScope.PER_MULTIBLOCK
                ? perMultiblockKey(player.getUniqueId(), normalizedMultiblock)
                : globalKey(player.getUniqueId());
        return readCounter(key);
    }

    @Override
    public int getLimit(Player player, String multiblockId) {
        if (player == null) {
            return Integer.MAX_VALUE;
        }
        Optional<MultiblockLimitDefinition> defOpt = resolver.resolve(player, multiblockId);
        if (defOpt.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int max = defOpt.get().max();
        return max < 0 ? Integer.MAX_VALUE : max;
    }

    @Override
    public void registerAssembly(Player player, String multiblockId) {
        if (player == null) {
            return;
        }
        registerAssembly(player.getUniqueId(), multiblockId);
    }

    @Override
    public void registerAssembly(UUID playerId, String multiblockId) {
        if (playerId == null) {
            return;
        }
        String normalizedMultiblock = normalizeMultiblock(multiblockId);
        synchronized (lock(playerId)) {
            increment(globalKey(playerId));
            if (!normalizedMultiblock.isBlank()) {
                increment(perMultiblockKey(playerId, normalizedMultiblock));
            }
        }
    }

    @Override
    public void unregisterAssembly(Player player, String multiblockId) {
        if (player == null) {
            return;
        }
        unregisterAssembly(player.getUniqueId(), multiblockId);
    }

    @Override
    public void unregisterAssembly(UUID playerId, String multiblockId) {
        if (playerId == null) {
            return;
        }
        String normalizedMultiblock = normalizeMultiblock(multiblockId);
        synchronized (lock(playerId)) {
            decrement(globalKey(playerId));
            if (!normalizedMultiblock.isBlank()) {
                decrement(perMultiblockKey(playerId, normalizedMultiblock));
            }
        }
    }

    private void increment(String key) {
        int current = readCounter(key);
        writeCounter(key, Math.max(current + 1, 0));
    }

    private void decrement(String key) {
        int current = readCounter(key);
        int next = Math.max(current - 1, 0);
        if (next <= 0) {
            store.delete(key, StorageRecordMeta.now("core"));
            return;
        }
        writeCounter(key, next);
    }

    private int readCounter(String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }
        Optional<StoredRecord> record = store.read(key);
        if (record.isEmpty() || record.get().payload() == null) {
            return 0;
        }
        try {
            String raw = new String(record.get().payload(), StandardCharsets.UTF_8).trim();
            int parsed = Integer.parseInt(raw);
            return Math.max(parsed, 0);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void writeCounter(String key, int value) {
        int safe = Math.max(value, 0);
        if (safe <= 0) {
            store.delete(key, StorageRecordMeta.now("core"));
            return;
        }
        byte[] payload = String.valueOf(safe).getBytes(StandardCharsets.UTF_8);
        store.write(key, payload, StorageRecordMeta.now("core"));
    }

    private Object lock(UUID playerId) {
        return locks.computeIfAbsent(playerId, unused -> new Object());
    }

    private static String normalizeMultiblock(String multiblockId) {
        if (multiblockId == null) {
            return "";
        }
        return multiblockId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String globalKey(UUID playerId) {
        return "limits:" + playerId + ":global";
    }

    private static String perMultiblockKey(UUID playerId, String multiblockId) {
        return "limits:" + playerId + ":" + normalizeMultiblock(multiblockId);
    }

    private static StorageSchema schema() {
        return new StorageSchema() {
            @Override
            public int schemaVersion() {
                return 1;
            }

            @Override
            public StorageSchemaMigrator migrator() {
                return (fromVersion, toVersion, payload) -> {
                    if (fromVersion == toVersion && toVersion == 1) {
                        return payload;
                    }
                    throw new IllegalStateException("Unsupported migration: " + fromVersion + "->" + toVersion);
                };
            }
        };
    }
}
