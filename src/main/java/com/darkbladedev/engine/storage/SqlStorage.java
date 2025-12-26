package com.darkbladedev.engine.storage;

import com.darkbladedev.engine.MultiBlockEngine;
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
import java.util.logging.Level;
import java.lang.reflect.Type;

public class SqlStorage implements StorageManager {

    private final MultiBlockEngine plugin;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();

    public SqlStorage(MultiBlockEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        // For simplicity using H2 or SQLite would be easier, but let's assume MySQL/SQLite
        // I'll use SQLite for a standalone plugin if no config provided, but standard practice is config.
        // For this task, I'll use a local SQLite file.
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/multiblocks.db");
        config.setPoolName("MultiBlockPool");
        
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
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database", e);
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
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public void saveInstance(MultiblockInstance instance) {
        if (!instance.type().persistent()) return;
        
        // Async usually, but for simplicity sync here or wrap in runTaskAsynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO multiblock_instances (type_id, world, x, y, z, facing, variables) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, instance.type().id());
                ps.setString(2, instance.anchorLocation().getWorld().getName());
                ps.setInt(3, instance.anchorLocation().getBlockX());
                ps.setInt(4, instance.anchorLocation().getBlockY());
                ps.setInt(5, instance.anchorLocation().getBlockZ());
                ps.setString(6, instance.facing().name());
                ps.setString(7, gson.toJson(instance.getVariables()));
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save multiblock instance", e);
            }
        });
    }

    @Override
    public void deleteInstance(MultiblockInstance instance) {
        if (!instance.type().persistent()) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM multiblock_instances WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                ps.setString(1, instance.anchorLocation().getWorld().getName());
                ps.setInt(2, instance.anchorLocation().getBlockX());
                ps.setInt(3, instance.anchorLocation().getBlockY());
                ps.setInt(4, instance.anchorLocation().getBlockZ());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete multiblock instance", e);
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
                    plugin.getLogger().warning("Unknown multiblock type in DB: " + typeId);
                    continue;
                }
                
                Location loc = new Location(world, x, y, z);
                BlockFace facing = BlockFace.NORTH;
                try {
                    facing = BlockFace.valueOf(facingName);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid facing in DB: " + facingName);
                }
                
                MultiblockState state = MultiblockState.ACTIVE;
                try {
                    if (stateName != null) {
                        state = MultiblockState.valueOf(stateName);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid state in DB: " + stateName);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to load multiblock instances", e);
        }
        
        return instances;
    }
}
