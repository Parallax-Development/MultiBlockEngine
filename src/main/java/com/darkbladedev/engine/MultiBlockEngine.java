package com.darkbladedev.engine;

import com.darkbladedev.engine.command.MultiblockCommand;
import com.darkbladedev.engine.listener.MultiblockListener;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.parser.MultiblockParser;
import com.darkbladedev.engine.storage.SqlStorage;
import com.darkbladedev.engine.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class MultiBlockEngine extends JavaPlugin {

    private static MultiBlockEngine instance;
    private MultiblockManager manager;
    private MultiblockParser parser;
    private StorageManager storage;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("MultiBlockEngine starting...");
        
        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Save default config
        saveDefaultConfig();

        // Initialize components
        manager = new MultiblockManager();
        parser = new MultiblockParser();
        storage = new SqlStorage(this);
        storage.init();
        manager.setStorage(storage);
        
        // Ensure directory exists
        File multiblockDir = new File(getDataFolder(), "multiblocks");
        if (!multiblockDir.exists()) {
            multiblockDir.mkdirs();
            // Create default example if empty
            saveResource("multiblocks/example_portal.yml", false);
        }
        
        // Load definitions
        List<MultiblockType> types = parser.loadAll(multiblockDir);
        for (MultiblockType type : types) {
            manager.registerType(type);
            getLogger().info("Loaded multiblock: " + type.id());
        }
        
        // Restore persisted instances
        Collection<MultiblockInstance> instances = storage.loadAll();
        for (MultiblockInstance inst : instances) {
            manager.registerInstance(inst);
        }
        getLogger().info("Restored " + instances.size() + " active instances.");
        
        // Register Listeners
        getServer().getPluginManager().registerEvents(new MultiblockListener(manager), this);
        
        // Register Commands
        MultiblockCommand cmd = new MultiblockCommand(this);
        getCommand("multiblock").setExecutor(cmd);
        getCommand("multiblock").setTabCompleter(cmd);
        
        // Start Ticking
        manager.startTicking(this);
        
        getLogger().info("MultiBlockEngine enabled with " + types.size() + " types.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.unregisterAll();
        }
        if (storage != null) {
            storage.close();
        }
        getLogger().info("MultiBlockEngine stopping...");
    }

    public static MultiBlockEngine getInstance() {
        return instance;
    }
    
    public MultiblockManager getManager() {
        return manager;
    }
    
    public MultiblockParser getParser() {
        return parser;
    }
}
