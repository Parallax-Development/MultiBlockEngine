package com.darkbladedev.engine.parser;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.model.matcher.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MultiblockParser {

    public List<MultiblockType> loadAll(File directory) {
        List<MultiblockType> types = new ArrayList<>();
        if (!directory.exists()) return types;

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return types;

        for (File file : files) {
            try {
                types.add(parse(file));
            } catch (Exception e) {
                MultiBlockEngine.getInstance().getLogger().log(Level.SEVERE, "Failed to parse multiblock file: " + file.getName(), e);
            }
        }
        return types;
    }

    public MultiblockType parse(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        String id = config.getString("id");
        if (id == null) throw new IllegalArgumentException("Missing 'id'");
        
        String version = config.getString("version");
        if (version == null) throw new IllegalArgumentException("Missing 'version'");

        // Parse controller matcher
        Object controllerObj = config.get("controller");
        if (controllerObj == null) throw new IllegalArgumentException("Missing 'controller'");
        BlockMatcher controllerMatcher = parseMatcher(controllerObj);
        
        // Parse pattern list
        List<PatternEntry> pattern = new ArrayList<>();
        List<?> patternList = config.getList("pattern");
        if (patternList == null) throw new IllegalArgumentException("Missing 'pattern'");

        for (Object obj : patternList) {
            if (obj instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                // Parse offset
                Vector offset = parseVector(map.get("offset"));
                // Parse matcher
                BlockMatcher matcher = parseMatcher(map.get("match"));
                pattern.add(new PatternEntry(offset, matcher));
            }
        }
        
        // Parse behavior config
        java.util.Map<String, Object> behaviorConfig = new java.util.HashMap<>();
        if (config.isConfigurationSection("behavior")) {
            behaviorConfig = config.getConfigurationSection("behavior").getValues(true);
        }
        
        return new MultiblockType(id, version, new Vector(0, 0, 0), controllerMatcher, pattern, true, behaviorConfig);
    }
    
    private Vector parseVector(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.size() >= 3) {
                double x = ((Number) list.get(0)).doubleValue();
                double y = ((Number) list.get(1)).doubleValue();
                double z = ((Number) list.get(2)).doubleValue();
                return new Vector(x, y, z);
            }
        }
        throw new IllegalArgumentException("Invalid vector format: " + obj);
    }

    private BlockMatcher parseMatcher(Object obj) {
        if (obj instanceof String) {
            String s = (String) obj;
            if (s.equalsIgnoreCase("AIR")) return new AirMatcher();
            if (s.startsWith("#")) {
                // Tag
                String tagName = s.substring(1);
                NamespacedKey key = NamespacedKey.fromString(tagName);
                if (key == null) throw new IllegalArgumentException("Invalid tag key: " + tagName);
                
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
                if (tag == null) throw new IllegalArgumentException("Unknown tag: " + tagName);
                return new TagMatcher(tag);
            } else {
                // Material
                Material mat = Material.matchMaterial(s);
                if (mat == null) throw new IllegalArgumentException("Unknown material: " + s);
                return new ExactMaterialMatcher(mat);
            }
        } else if (obj instanceof List) {
            // AnyOf
            List<BlockMatcher> matchers = new ArrayList<>();
            for (Object o : (List<?>) obj) {
                matchers.add(parseMatcher(o));
            }
            return new AnyOfMatcher(matchers);
        }
        throw new IllegalArgumentException("Invalid matcher format: " + obj);
    }
}
