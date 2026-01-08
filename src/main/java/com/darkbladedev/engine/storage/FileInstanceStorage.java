package com.darkbladedev.engine.storage;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.persistence.PersistentStorageService;
import com.darkbladedev.engine.api.persistence.StorageRecordMeta;
import com.darkbladedev.engine.api.persistence.StorageSchema;
import com.darkbladedev.engine.api.persistence.StorageStore;
import com.darkbladedev.engine.api.persistence.StoredRecord;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;
import com.darkbladedev.engine.model.MultiblockType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FileInstanceStorage implements StorageManager {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final MultiBlockEngine plugin;
    private final PersistentStorageService persistence;
    private final Gson gson = new Gson();

    private StorageStore store;

    public FileInstanceStorage(MultiBlockEngine plugin, PersistentStorageService persistence) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.persistence = Objects.requireNonNull(persistence, "persistence");
    }

    @Override
    public void init() {
        StorageSchema schema = new StorageSchema() {
            @Override
            public int schemaVersion() {
                return 1;
            }

            @Override
            public StorageSchema.StorageSchemaMigrator migrator() {
                return (fromVersion, toVersion, payload) -> {
                    if (fromVersion == toVersion && toVersion == 1) {
                        return payload;
                    }
                    throw new IllegalStateException("Unsupported migration: " + fromVersion + "->" + toVersion);
                };
            }
        };

        this.store = persistence
            .namespace("core")
            .domain("multiblocks")
            .store("instances", schema);
    }

    @Override
    public void close() {
        try {
            persistence.flush();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void saveInstance(MultiblockInstance instance) {
        if (instance == null || instance.type() == null || !instance.type().persistent()) {
            return;
        }
        if (store == null) {
            return;
        }

        Location anchor = instance.anchorLocation();
        World world = anchor == null ? null : anchor.getWorld();
        if (world == null) {
            return;
        }

        String key = key(world.getName(), anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());

        Map<String, Object> record = new HashMap<>();
        record.put("type", instance.type().id());
        record.put("world", world.getName());
        record.put("x", anchor.getBlockX());
        record.put("y", anchor.getBlockY());
        record.put("z", anchor.getBlockZ());
        record.put("facing", instance.facing() == null ? BlockFace.NORTH.name() : instance.facing().name());
        record.put("state", instance.state() == null ? MultiblockState.ACTIVE.name() : instance.state().name());
        record.put("variables", sanitize(instance.getVariables()));

        byte[] payload = gson.toJson(record).getBytes(StandardCharsets.UTF_8);
        store.write(key, payload, StorageRecordMeta.now("core"));
    }

    @Override
    public void deleteInstance(MultiblockInstance instance) {
        if (instance == null || instance.type() == null || !instance.type().persistent()) {
            return;
        }
        if (store == null) {
            return;
        }

        Location anchor = instance.anchorLocation();
        World world = anchor == null ? null : anchor.getWorld();
        if (world == null) {
            return;
        }

        String key = key(world.getName(), anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
        store.delete(key, StorageRecordMeta.now("core"));
    }

    @Override
    public java.util.Collection<MultiblockInstance> loadAll() {
        if (store == null) {
            return List.of();
        }

        List<MultiblockInstance> out = new ArrayList<>();
        Map<String, StoredRecord> all = store.readAll();
        for (StoredRecord r : all.values()) {
            if (r == null || r.payload() == null) {
                continue;
            }
            try {
                Map<String, Object> map = gson.fromJson(new String(r.payload(), StandardCharsets.UTF_8), MAP_TYPE);
                if (map == null) {
                    continue;
                }

                String typeId = asString(map.get("type"));
                String worldName = asString(map.get("world"));
                int x = asInt(map.get("x"));
                int y = asInt(map.get("y"));
                int z = asInt(map.get("z"));
                String facingName = asString(map.get("facing"));
                String stateName = asString(map.get("state"));
                Map<String, Object> vars = asMap(map.get("variables"));

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }

                Optional<MultiblockType> typeOpt = plugin.getManager().getType(typeId);
                if (typeOpt.isEmpty()) {
                    continue;
                }

                BlockFace facing;
                try {
                    facing = facingName == null ? BlockFace.NORTH : BlockFace.valueOf(facingName);
                } catch (Exception e) {
                    facing = BlockFace.NORTH;
                }

                MultiblockState state;
                try {
                    state = stateName == null ? MultiblockState.ACTIVE : MultiblockState.valueOf(stateName);
                } catch (Exception e) {
                    state = MultiblockState.ACTIVE;
                }

                MultiblockInstance inst = new MultiblockInstance(typeOpt.get(), new Location(world, x, y, z), facing, state, vars);
                out.add(inst);
            } catch (Exception ignored) {
            }
        }

        return out;
    }

    private static String key(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    private static Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                out.put(String.valueOf(e.getKey()), sanitize(e.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object v : list) {
                out.add(sanitize(v));
            }
            return out;
        }
        return String.valueOf(value);
    }

    private static String asString(Object v) {
        return v instanceof String s ? s : (v == null ? null : String.valueOf(v));
    }

    private static int asInt(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private static Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return Map.of();
    }
}
