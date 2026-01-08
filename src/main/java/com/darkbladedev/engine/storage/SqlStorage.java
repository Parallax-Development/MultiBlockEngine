package com.darkbladedev.engine.storage;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;
import com.darkbladedev.engine.model.MultiblockType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.Base64;
import java.util.UUID;
import java.lang.reflect.Type;

public class SqlStorage implements StorageManager {

    private final MultiBlockEngine plugin;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private ExecutorService executor;

    public SqlStorage(MultiBlockEngine plugin) {
        this.plugin = plugin;
    }

    private void log(LogPhase phase, LogLevel level, String message, Throwable throwable, LogKv... fields) {
        CoreLogger core = plugin.getLoggingManager() != null ? plugin.getLoggingManager().core() : null;
        if (core != null) {
            core.logInternal(new LogScope.Core(), phase, level, message, throwable, fields, Set.of());
            return;
        }

        java.util.logging.Level jul = switch (level) {
            case TRACE, DEBUG, INFO -> java.util.logging.Level.INFO;
            case WARN -> java.util.logging.Level.WARNING;
            case ERROR, FATAL -> java.util.logging.Level.SEVERE;
        };
        plugin.getLogger().log(jul, message, throwable);
    }

    @Override
    public void init() {
        closing.set(false);

        if (executor == null) {
            ThreadFactory tf = r -> {
                Thread t = new Thread(r, "MBE-SqlStorage");
                t.setDaemon(true);
                return t;
            };
            executor = Executors.newSingleThreadExecutor(tf);
        }

        HikariConfig config = new HikariConfig();
        // For simplicity using H2 or SQLite would be easier, but let's assume MySQL/SQLite
        // I'll use SQLite for a standalone plugin if no config provided, but standard practice is config.
        // For this task, I'll use a local SQLite file.
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/multiblocks.db");
        config.setPoolName("MultiBlockPool");

        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);
        config.setValidationTimeout(2_000);
        
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection()) {
            // Version Table
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS schema_version (version INT NOT NULL);")) {
                ps.execute();
            }
            
            // Check current version
            int currentVersion = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT version FROM schema_version LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentVersion = rs.getInt("version");
                } else {
                    // Initialize if empty
                     try (PreparedStatement insert = conn.prepareStatement("INSERT INTO schema_version (version) VALUES (0)")) {
                         insert.execute();
                     }
                }
            }
            
            // Run Migrations
            if (currentVersion < 1) {
                 try (PreparedStatement ps = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS multiblock_instances (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "type_id VARCHAR(64) NOT NULL," +
                                "world VARCHAR(64) NOT NULL," +
                                "x INT NOT NULL," +
                                "y INT NOT NULL," +
                                "z INT NOT NULL," +
                                "facing VARCHAR(16) NOT NULL DEFAULT 'NORTH'," +
                                "state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'" +
                                ");")) {
                    ps.execute();
                }
                updateVersion(conn, 1);
            }
            
            if (currentVersion < 2) {
                 try (PreparedStatement ps = conn.prepareStatement(
                        "ALTER TABLE multiblock_instances ADD COLUMN variables TEXT DEFAULT '{}'")) {
                    ps.execute();
                }
                updateVersion(conn, 2);
            }

            if (currentVersion < 3) {
                try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM multiblock_instances WHERE id NOT IN (" +
                        "SELECT MAX(id) FROM multiblock_instances GROUP BY world, x, y, z" +
                    ")")) {
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_multiblock_instances_anchor ON multiblock_instances(world, x, y, z)")) {
                    ps.execute();
                }

                updateVersion(conn, 3);
            }
            
        } catch (SQLException e) {
            log(LogPhase.BOOT, LogLevel.ERROR, "Could not initialize database", e);
        }
    }
    
    private void updateVersion(Connection conn, int newVersion) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE schema_version SET version = ?")) {
            ps.setInt(1, newVersion);
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        closing.set(true);

        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(750, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    @Override
    public void saveInstance(MultiblockInstance instance) {
        if (instance == null || instance.type() == null || !instance.type().persistent()) {
            return;
        }

        if (closing.get() || executor == null || dataSource == null || !plugin.isEnabled()) {
            return;
        }

        Location anchor = instance.anchorLocation();
        World world = anchor == null ? null : anchor.getWorld();
        if (world == null) {
            return;
        }

        String typeId = instance.type().id();
        String worldName = world.getName();
        int x = anchor.getBlockX();
        int y = anchor.getBlockY();
        int z = anchor.getBlockZ();
        String facing = instance.facing() == null ? BlockFace.NORTH.name() : instance.facing().name();
        String state = instance.state() == null ? MultiblockState.ACTIVE.name() : instance.state().name();
        Map<String, Object> variables = instance.getVariables() == null ? Map.of() : new HashMap<>(instance.getVariables());

        executor.execute(() -> {
            if (closing.get()) {
                return;
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO multiblock_instances (type_id, world, x, y, z, facing, state, variables) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                                 "ON CONFLICT(world, x, y, z) DO UPDATE SET " +
                                 "type_id = excluded.type_id, " +
                                 "facing = excluded.facing, " +
                                 "state = excluded.state, " +
                                 "variables = excluded.variables")) {
                ps.setString(1, typeId);
                ps.setString(2, worldName);
                ps.setInt(3, x);
                ps.setInt(4, y);
                ps.setInt(5, z);
                ps.setString(6, facing);
                ps.setString(7, state);

                SanitizationStats stats = new SanitizationStats();
                Map<String, Object> sanitized = sanitizeVariablesForJson(variables, stats);
                if (stats.changedCount() > 0) {
                    log(LogPhase.RUNTIME, LogLevel.WARN, "Sanitized multiblock variables before save", null,
                            LogKv.kv("type", typeId),
                            LogKv.kv("world", worldName),
                            LogKv.kv("x", x),
                            LogKv.kv("y", y),
                            LogKv.kv("z", z),
                            LogKv.kv("changed", stats.changedCount())
                    );
                }

                String json;
                try {
                    json = gson.toJson(sanitized);
                } catch (RuntimeException ex) {
                    json = "{}";
                    log(LogPhase.RUNTIME, LogLevel.ERROR, "Failed to serialize multiblock variables", ex,
                            LogKv.kv("type", typeId),
                            LogKv.kv("world", worldName),
                            LogKv.kv("x", x),
                            LogKv.kv("y", y),
                            LogKv.kv("z", z)
                    );
                }

                ps.setString(8, json);
                ps.executeUpdate();
            } catch (SQLException e) {
                if (closing.get() || Thread.currentThread().isInterrupted() || !plugin.isEnabled()) {
                    return;
                }
                log(LogPhase.RUNTIME, LogLevel.ERROR, "Failed to save multiblock instance", e,
                        LogKv.kv("type", typeId),
                        LogKv.kv("world", worldName),
                        LogKv.kv("x", x),
                        LogKv.kv("y", y),
                        LogKv.kv("z", z)
                );
            }
        });
    }

    private static final class SanitizationStats {
        private final java.util.concurrent.atomic.AtomicInteger changed = new java.util.concurrent.atomic.AtomicInteger();

        private void inc() {
            changed.incrementAndGet();
        }

        private int changedCount() {
            return changed.get();
        }
    }

    private Map<String, Object> sanitizeVariablesForJson(Map<String, Object> input, SanitizationStats stats) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        IdentityHashMap<Object, Boolean> visiting = new IdentityHashMap<>();
        Object out = sanitizeJsonValue(input, visiting, stats);
        if (out instanceof Map<?, ?> map) {
            Map<String, Object> cast = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() instanceof String k) {
                    cast.put(k, e.getValue());
                }
            }
            return cast;
        }
        return Map.of();
    }

    private Object sanitizeJsonValue(Object value, IdentityHashMap<Object, Boolean> visiting, SanitizationStats stats) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Character c) {
            stats.inc();
            return c.toString();
        }
        if (value instanceof UUID uuid) {
            stats.inc();
            return uuid.toString();
        }
        if (value instanceof Class<?> cls) {
            stats.inc();
            return cls.getName();
        }
        if (value instanceof Enum<?> en) {
            stats.inc();
            return en.name();
        }
        if (value instanceof byte[] bytes) {
            stats.inc();
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof org.bukkit.inventory.ItemStack stack) {
            stats.inc();
            org.bukkit.inventory.ItemStack single = stack.clone();
            if (single.getAmount() != 1) {
                single.setAmount(1);
            }
            return sanitizeJsonValue(single.serialize(), visiting, stats);
        }

        if (visiting.put(value, Boolean.TRUE) != null) {
            stats.inc();
            return String.valueOf(value);
        }
        try {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> out = new HashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String key;
                    if (e.getKey() instanceof String s) {
                        key = s;
                    } else {
                        key = String.valueOf(e.getKey());
                        stats.inc();
                    }
                    Object next = sanitizeJsonValue(e.getValue(), visiting, stats);
                    out.put(key, next);
                }
                return out;
            }
            if (value instanceof Collection<?> coll) {
                List<Object> out = new ArrayList<>(coll.size());
                for (Object o : coll) {
                    out.add(sanitizeJsonValue(o, visiting, stats));
                }
                return out;
            }
            if (value.getClass().isArray()) {
                if (value instanceof Object[] arr) {
                    List<Object> out = new ArrayList<>(arr.length);
                    for (Object o : arr) {
                        out.add(sanitizeJsonValue(o, visiting, stats));
                    }
                    stats.inc();
                    return out;
                }
                stats.inc();
                return String.valueOf(value);
            }
        } finally {
            visiting.remove(value);
        }

        stats.inc();
        return String.valueOf(value);
    }

    @Override
    public void deleteInstance(MultiblockInstance instance) {
        if (instance == null || instance.type() == null || !instance.type().persistent()) {
            return;
        }

        if (closing.get() || executor == null || dataSource == null || !plugin.isEnabled()) {
            return;
        }

        Location anchor = instance.anchorLocation();
        World world = anchor == null ? null : anchor.getWorld();
        if (world == null) {
            return;
        }

        String typeId = instance.type().id();
        String worldName = world.getName();
        int x = anchor.getBlockX();
        int y = anchor.getBlockY();
        int z = anchor.getBlockZ();

        executor.execute(() -> {
            if (closing.get()) {
                return;
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM multiblock_instances WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                ps.setString(1, worldName);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);
                ps.executeUpdate();
            } catch (SQLException e) {
                if (closing.get() || Thread.currentThread().isInterrupted() || !plugin.isEnabled()) {
                    return;
                }
                log(LogPhase.RUNTIME, LogLevel.ERROR, "Failed to delete multiblock instance", e,
                        LogKv.kv("type", typeId),
                        LogKv.kv("world", worldName),
                        LogKv.kv("x", x),
                        LogKv.kv("y", y),
                        LogKv.kv("z", z)
                );
            }
        });
    }

    @Override
    public Collection<MultiblockInstance> loadAll() {
        List<MultiblockInstance> instances = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM multiblock_instances");
             ResultSet rs = ps.executeQuery()) {
            
            MultiblockManager manager = plugin.getManager();
            
            while (rs.next()) {
                String typeId = rs.getString("type_id");
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String facingName = rs.getString("facing");
                String stateName = rs.getString("state");
                String variablesJson = rs.getString("variables");
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue; // World not loaded
                
                Optional<MultiblockType> typeOpt = manager.getType(typeId);
                if (typeOpt.isEmpty()) {
                    log(LogPhase.LOAD, LogLevel.WARN, "Unknown multiblock type in DB", null, LogKv.kv("type", typeId));
                    continue;
                }
                
                Location loc = new Location(world, x, y, z);
                BlockFace facing = BlockFace.NORTH;
                try {
                    facing = BlockFace.valueOf(facingName);
                } catch (IllegalArgumentException ignored) {
                    log(LogPhase.LOAD, LogLevel.WARN, "Invalid facing in DB", null, LogKv.kv("facing", facingName));
                }
                
                MultiblockState state = MultiblockState.ACTIVE;
                try {
                    if (stateName != null) {
                        state = MultiblockState.valueOf(stateName);
                    }
                } catch (IllegalArgumentException ignored) {
                    log(LogPhase.LOAD, LogLevel.WARN, "Invalid state in DB", null, LogKv.kv("state", stateName));
                }
                
                Map<String, Object> variables = new HashMap<>();
                if (variablesJson != null && !variablesJson.isEmpty() && !variablesJson.equals("{}")) {
                     Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                     variables = gson.fromJson(variablesJson, mapType);
                     
                     // GSON parses numbers as Double by default, we might need to fix integers later if strict typing is required.
                     // For now, it's fine.
                }
                
                instances.add(new MultiblockInstance(typeOpt.get(), loc, facing, state, variables));
            }
        } catch (SQLException e) {
            log(LogPhase.LOAD, LogLevel.ERROR, "Failed to load multiblock instances", e);
        }
        
        return instances;
    }
}
