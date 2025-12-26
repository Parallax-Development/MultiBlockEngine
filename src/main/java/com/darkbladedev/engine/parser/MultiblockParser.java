package com.darkbladedev.engine.parser;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MultiblockState;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.model.action.ConsoleCommandAction;
import com.darkbladedev.engine.model.action.SendMessageAction;
import com.darkbladedev.engine.model.action.SetStateAction;
import com.darkbladedev.engine.model.condition.Condition;
import com.darkbladedev.engine.model.condition.StateCondition;
import com.darkbladedev.engine.model.condition.VariableCondition;
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
                // Parse optional
                boolean optional = map.containsKey("optional") && Boolean.TRUE.equals(map.get("optional"));
                
                pattern.add(new PatternEntry(offset, matcher, optional));
            }
        }
        
        // Parse behavior config
        java.util.Map<String, Object> behaviorConfig = new java.util.HashMap<>();
        if (config.isConfigurationSection("behavior")) {
            behaviorConfig = config.getConfigurationSection("behavior").getValues(true);
        }
        
        // Parse default variables
        java.util.Map<String, Object> defaultVariables = new java.util.HashMap<>();
        if (config.isConfigurationSection("variables")) {
            defaultVariables = config.getConfigurationSection("variables").getValues(false);
        }
        
        // Parse Actions
        List<Action> onCreateActions = parseActions(config, "actions.on_create");
        List<Action> onTickActions = parseActions(config, "actions.on_tick");
        List<Action> onInteractActions = parseActions(config, "actions.on_interact");
        List<Action> onBreakActions = parseActions(config, "actions.on_break");
        
        int tickInterval = config.getInt("tick_interval", 20);
        
        return new MultiblockType(id, version, new Vector(0, 0, 0), controllerMatcher, pattern, true, behaviorConfig, defaultVariables, onCreateActions, onTickActions, onInteractActions, onBreakActions, tickInterval);
    }
    
    private List<Action> parseActions(YamlConfiguration config, String path) {
        List<Action> actions = new ArrayList<>();
        if (config.contains(path)) {
             List<?> actionList = config.getList(path);
             for (Object obj : actionList) {
                 if (obj instanceof java.util.Map) {
                     java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                     
                     // Parse conditions if present
                     List<Condition> conditions = new ArrayList<>();
                     if (map.containsKey("conditions")) {
                         List<?> condList = (List<?>) map.get("conditions");
                         for (Object condObj : condList) {
                             if (condObj instanceof java.util.Map) {
                                 java.util.Map<?, ?> condMap = (java.util.Map<?, ?>) condObj;
                                 String condType = (String) condMap.get("type");
                                 
                                 if ("state".equalsIgnoreCase(condType)) {
                                     conditions.add(new StateCondition(MultiblockState.valueOf((String) condMap.get("value"))));
                                 } else if ("variable".equalsIgnoreCase(condType)) {
                                     conditions.add(new VariableCondition((String) condMap.get("key"), condMap.get("value")));
                                 }
                             }
                         }
                     }
                     
                     String type = (String) map.get("type");
                     Action action = null;
                     
                     if ("message".equalsIgnoreCase(type)) {
                         action = new SendMessageAction((String) map.get("value"));
                     } else if ("command".equalsIgnoreCase(type)) {
                         action = new ConsoleCommandAction((String) map.get("value"));
                     } else if ("set_state".equalsIgnoreCase(type)) {
                         action = new SetStateAction(MultiblockState.valueOf((String) map.get("value")));
                     }
                     
                     if (action != null) {
                         // Wrap with conditions if any
                         if (!conditions.isEmpty()) {
                             Action finalAction = action;
                             actions.add(instance -> {
                                 for (Condition c : conditions) {
                                     if (!c.check(instance)) return;
                                 }
                                 finalAction.execute(instance);
                             });
                         } else {
                             actions.add(action);
                         }
                     }
                 }
             }
        }
        return actions;
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
            } else if (s.contains("[")) {
                // BlockData (e.g. "minecraft:chest[facing=north]")
                try {
                    // Normalize input if needed (Bukkit expects "minecraft:name[data]")
                    // If user provides "CHEST[facing=north]", it might fail if not lowercase/namespaced
                    // But Bukkit.createBlockData handles standard formats.
                    org.bukkit.block.data.BlockData data = Bukkit.createBlockData(s.toLowerCase());
                    return new BlockDataMatcher(data);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid BlockData string: " + s, e);
                }
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
