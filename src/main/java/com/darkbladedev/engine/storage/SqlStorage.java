package com.darkbladedev.engine.storage;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class SqlStorage implements StorageManager {

    private final MultiBlockEngine plugin;
    private HikariDataSource dataSource;

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
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS multiblock_instances (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "type_id VARCHAR(64) NOT NULL," +
                            "world VARCHAR(64) NOT NULL," +
                            "x INT NOT NULL," +
                            "y INT NOT NULL," +
                            "z INT NOT NULL" +
                            ");")) {
                ps.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database", e);
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
                         "INSERT INTO multiblock_instances (type_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, instance.type().id());
                ps.setString(2, instance.anchorLocation().getWorld().getName());
                ps.setInt(3, instance.anchorLocation().getBlockX());
                ps.setInt(4, instance.anchorLocation().getBlockY());
                ps.setInt(5, instance.anchorLocation().getBlockZ());
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
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue; // World not loaded
                
                Optional<MultiblockType> typeOpt = manager.getType(typeId);
                if (typeOpt.isEmpty()) {
                    plugin.getLogger().warning("Unknown multiblock type in DB: " + typeId);
                    continue;
                }
                
                Location loc = new Location(world, x, y, z);
                instances.add(new MultiblockInstance(typeOpt.get(), loc));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load multiblock instances", e);
        }
        
        return instances;
    }
}
