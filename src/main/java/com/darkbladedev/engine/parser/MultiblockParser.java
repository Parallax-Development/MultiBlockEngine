package com.darkbladedev.engine.parser;

import com.darkbladedev.engine.api.impl.MultiblockAPIImpl;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.DisplayNameConfig;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.model.action.*;
import com.darkbladedev.engine.model.condition.*;
import com.darkbladedev.engine.model.matcher.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class MultiblockParser {
    
    private final MultiblockAPIImpl api;
    private final CoreLogger log;
    
    private record RawDefinition(String id, File file, YamlConfiguration config) {}
    
    public MultiblockParser(MultiblockAPIImpl api, CoreLogger log) {
        this.api = api;
        this.log = Objects.requireNonNull(log, "log");
        registerDefaults();
    }
    
    @SuppressWarnings("unchecked")
    private void registerDefaults() {
        // Actions
        api.registerAction("message", map -> new SendMessageAction((String) map.get("value"), map.get("target")));
        api.registerAction("command", map -> new ConsoleCommandAction((String) map.get("value")));
        api.registerAction("set_state", map -> new SetStateAction(MultiblockState.valueOf((String) map.get("value"))));
        api.registerAction("set_variable", map -> new SetVariableAction((String) map.get("key"), map.get("value")));
        api.registerAction("modify_variable", map -> {
            String opStr = (String) map.get("operation");
            ModifyVariableAction.Operation op = ModifyVariableAction.Operation.valueOf(opStr.toUpperCase());
            double amount = 0;
            Object amountObj = map.get("amount");
            if (amountObj instanceof Number n) {
                amount = n.doubleValue();
            }
            return new ModifyVariableAction((String) map.get("key"), amount, op);
        });
        
        api.registerAction("conditional", map -> {
            List<Condition> conditions = new ArrayList<>();
            if (map.containsKey("conditions")) {
                List<?> condList = (List<?>) map.get("conditions");
                for (Object condObj : condList) {
                    if (condObj instanceof Map) {
                        Map<?, ?> condMap = (Map<?, ?>) condObj;
                        String condType = (String) condMap.get("type");
                        Function<Map<String, Object>, Condition> factory = api.getConditionFactory(condType);
                        if (factory != null) {
                            conditions.add(factory.apply((Map<String, Object>) condMap));
                        }
                    }
                }
            }
            
            List<Action> thenActions = new ArrayList<>();
            if (map.containsKey("then")) {
                thenActions = parseActionList((List<?>) map.get("then"));
            }
            
            List<Action> elseActions = new ArrayList<>();
            if (map.containsKey("else")) {
                elseActions = parseActionList((List<?>) map.get("else"));
            }
            
            return new ConditionalAction(conditions, thenActions, elseActions);
        });
        
        api.registerAction("spawn_item", map -> {
            Material mat = Material.matchMaterial((String) map.get("material"));
            int amount = (int) map.getOrDefault("amount", 1);
            Vector offset = parseVector(map.getOrDefault("offset", List.of(0, 1, 0)));
            return new SpawnItemAction(mat, amount, offset);
        });
        
        api.registerAction("spawn_entity", map -> {
            org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(((String) map.get("entity_type")).toUpperCase());
            Vector offset = parseVector(map.getOrDefault("offset", List.of(0, 1, 0)));
            String name = (String) map.get("name");
            return new SpawnEntityAction(type, offset, name);
        });
        
        api.registerAction("title", map -> {
            String title = (String) map.getOrDefault("title", "");
            String subtitle = (String) map.getOrDefault("subtitle", "");
            int fadeIn = (int) map.getOrDefault("fade_in", 10);
            int stay = (int) map.getOrDefault("stay", 70);
            int fadeOut = (int) map.getOrDefault("fade_out", 20);
            Object target = map.get("target");
            return new TitleAction(title, subtitle, fadeIn, stay, fadeOut, target);
        });
        
        api.registerAction("actionbar", map -> new ActionBarAction((String) map.get("message"), map.get("target")));
        
        api.registerAction("teleport", map -> {
            Vector offset = parseVector(map.getOrDefault("offset", List.of(0, 0, 0)));
            Object target = map.get("target");
            return new TeleportAction(offset, target);
        });
        
        // Conditions
        api.registerCondition("state", map -> new StateCondition(MultiblockState.valueOf((String) map.get("value"))));
        api.registerCondition("variable", map -> {
             String opStr = (String) map.get("comparison");
             VariableCondition.Comparison comp = VariableCondition.Comparison.EQUALS;
             if (opStr != null) {
                 comp = VariableCondition.Comparison.valueOf(opStr.toUpperCase());
             }
             return new VariableCondition((String) map.get("key"), map.get("value"), comp);
        });
        
        api.registerCondition("permission", map -> new PlayerPermissionCondition((String) map.get("value")));
        api.registerCondition("sneaking", map -> new PlayerSneakingCondition((boolean) map.getOrDefault("value", true)));

        api.registerMatcher("tag", tagName -> {
            NamespacedKey key = NamespacedKey.fromString(tagName);
            if (key == null) {
                throw new IllegalArgumentException("Invalid tag key: " + tagName);
            }
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
            if (tag == null) {
                throw new IllegalArgumentException("Unknown tag: " + tagName);
            }
            return new TagMatcher(tag);
        });
    }

    public List<MultiblockType> loadAll(File directory) {
        Map<String, RawDefinition> rawDefinitions = new HashMap<>();
        List<MultiblockType> types = new ArrayList<>();
        
        if (!directory.exists()) return types;

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return types;

        // 1. Load all raw definitions
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id");
                if (id == null) {
                    log.logInternal(new LogScope.Core(), LogPhase.LOAD, LogLevel.WARN, "Multiblock file missing id (skipping)", null, new LogKv[] {
                        LogKv.kv("file", file.getName())
                    }, Set.of());
                    continue;
                }
                rawDefinitions.put(id, new RawDefinition(id, file, config));
            } catch (Exception e) {
                log.logInternal(new LogScope.Core(), LogPhase.LOAD, LogLevel.ERROR, "Failed to load multiblock file", e, new LogKv[] {
                    LogKv.kv("file", file.getName())
                }, Set.of());
            }
        }

        // 2. Resolve inheritance
        Map<String, YamlConfiguration> resolvedConfigs = new HashMap<>();
        Set<String> resolving = new HashSet<>();
        Set<String> resolved = new HashSet<>();
        
        for (String id : rawDefinitions.keySet()) {
            try {
                resolve(id, rawDefinitions, resolvedConfigs, resolving, resolved);
            } catch (Exception e) {
                log.logInternal(new LogScope.Core(), LogPhase.LOAD, LogLevel.ERROR, "Error resolving template", e, new LogKv[] {
                    LogKv.kv("id", id)
                }, Set.of());
            }
        }
        
        // 3. Parse resolved configs
        for (String id : resolvedConfigs.keySet()) {
            try {
                // File originalFile = rawDefinitions.get(id).file();
                types.add(parse(resolvedConfigs.get(id))); 
            } catch (Exception e) {
                log.logInternal(new LogScope.Core(), LogPhase.LOAD, LogLevel.ERROR, "Failed to parse resolved multiblock", e, new LogKv[] {
                    LogKv.kv("id", id)
                }, Set.of());
            }
        }
        return types;
    }

    private void resolve(String id, Map<String, RawDefinition> raw, Map<String, YamlConfiguration> resolvedConfigs, Set<String> resolving, Set<String> resolved) {
        if (resolved.contains(id)) return;
        if (resolving.contains(id)) throw new IllegalStateException("Circular dependency detected: " + resolving + " -> " + id);
        
        resolving.add(id);
        
        RawDefinition def = raw.get(id);
        if (def == null) {
             throw new IllegalStateException("Definition not found for id: " + id);
        }
        
        YamlConfiguration config = def.config();
        String parentId = config.getString("extends");
        
        YamlConfiguration finalConfig;
        
        if (parentId != null) {
            if (!raw.containsKey(parentId)) {
                throw new IllegalStateException("Missing parent: " + parentId + " required by " + id + " (in " + def.file().getName() + ")");
            }
            resolve(parentId, raw, resolvedConfigs, resolving, resolved);
            YamlConfiguration parentConfig = resolvedConfigs.get(parentId);
            
            finalConfig = new YamlConfiguration();
            deepMerge(finalConfig, parentConfig);
            deepMerge(finalConfig, config);
            
            // Remove extends key from final object
            finalConfig.set("extends", null);
        } else {
            finalConfig = config;
        }
        
        resolvedConfigs.put(id, finalConfig);
        resolving.remove(id);
        resolved.add(id);
    }
    
    private void deepMerge(ConfigurationSection target, ConfigurationSection source) {
        for (String key : source.getKeys(false)) {
            Object sourceValue = source.get(key);
            Object targetValue = target.get(key);
            
            if (sourceValue instanceof ConfigurationSection sourceSection) {
                if (targetValue instanceof ConfigurationSection targetSection) {
                    deepMerge(targetSection, sourceSection);
                } else {
                    ConfigurationSection newSection = target.createSection(key);
                    deepMerge(newSection, sourceSection);
                }
            } else {
                // Scalar or List -> Overwrite
                target.set(key, sourceValue);
            }
        }
    }

    public MultiblockType parse(File file) {
        return parse(YamlConfiguration.loadConfiguration(file));
    }
    
    public MultiblockType parse(YamlConfiguration config) {
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
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
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
        Map<String, Object> behaviorConfig = new HashMap<>();
        if (config.isConfigurationSection("behavior")) {
            behaviorConfig = config.getConfigurationSection("behavior").getValues(true);
        }
        
        // Parse default variables
        Map<String, Object> defaultVariables = new HashMap<>();
        if (config.isConfigurationSection("variables")) {
            defaultVariables = config.getConfigurationSection("variables").getValues(false);
        }
        
        // Parse Actions
        List<Action> onCreateActions = parseActions(config, "actions.on_create");
        List<Action> onTickActions = parseActions(config, "actions.on_tick");
        List<Action> onInteractActions = parseActions(config, "actions.on_interact");
        List<Action> onBreakActions = parseActions(config, "actions.on_break");
        
        DisplayNameConfig displayName = null;
        if (config.contains("display_name")) {
            if (config.isConfigurationSection("display_name")) {
                ConfigurationSection section = config.getConfigurationSection("display_name");
                String text = section.getString("text");
                boolean visible = section.getBoolean("visible", true);
                String method = section.getString("display_method", "hologram");
                displayName = new DisplayNameConfig(text, visible, method);
            } else {
                // String sugar
                displayName = new DisplayNameConfig(config.getString("display_name"), true, "hologram");
            }
        }
        
        int tickInterval = config.getInt("tick_interval", 20);
        
        return new MultiblockType(id, version, new Vector(0, 0, 0), controllerMatcher, pattern, true, behaviorConfig, defaultVariables, onCreateActions, onTickActions, onInteractActions, onBreakActions, displayName, tickInterval);
    }
    
    private List<Action> parseActions(YamlConfiguration config, String path) {
        List<Action> actions = new ArrayList<>();
        if (config.contains(path)) {
             List<?> actionList = config.getList(path);
             actions.addAll(parseActionList(actionList));
        }
        return actions;
    }

    @SuppressWarnings("unchecked")
    private List<Action> parseActionList(List<?> actionList) {
        List<Action> actions = new ArrayList<>();
        for (Object obj : actionList) {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                
                // Parse conditions if present
                List<Condition> conditions = new ArrayList<>();
                if (map.containsKey("conditions")) {
                    List<?> condList = (List<?>) map.get("conditions");
                    for (Object condObj : condList) {
                        if (condObj instanceof Map) {
                            Map<?, ?> condMap = (Map<?, ?>) condObj;
                            String condType = (String) condMap.get("type");
                            
                            Function<Map<String, Object>, Condition> factory = api.getConditionFactory(condType);
                            if (factory != null) {
                                // Unsafe cast, but YAML parser gives us Map<String, Object> effectively
                                conditions.add(factory.apply((Map<String, Object>) condMap));
                            } else {
                                log.logInternal(new LogScope.Core(), LogPhase.LOAD, LogLevel.WARN, "Unknown condition type", null, new LogKv[] {
                                    LogKv.kv("type", condType)
                                }, Set.of());
                            }
                        }
                    }
                }
                
                String type = (String) map.get("type");
                Action action = null;
                
                Function<Map<String, Object>, Action> actionFactory = api.getActionFactory(type);
                if (actionFactory != null) {
                    action = actionFactory.apply((Map<String, Object>) map);
                } else {
                    log.logInternal(new LogScope.Core(), LogPhase.LOAD, LogLevel.WARN, "Unknown action type", null, new LogKv[] {
                        LogKv.kv("type", type)
                    }, Set.of());
                }
                
                if (action != null) {
                    // Wrap with conditions if any
                    if (!conditions.isEmpty()) {
                        Action finalAction = action;
                        Action conditional = new Action() {
                            @Override
                            public void execute(MultiblockInstance instance, org.bukkit.entity.Player player) {
                                for (Condition c : conditions) {
                                    if (!c.check(instance, player)) return;
                                }
                                finalAction.execute(instance, player);
                            }
                            
                            @Override
                            public void execute(MultiblockInstance instance) {
                                execute(instance, null);
                            }
                        };
                        actions.add(Action.owned(finalAction.ownerId(), finalAction.typeKey(), conditional));
                    } else {
                        actions.add(action);
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

            int prefixIdx = s.indexOf(':');
            if (prefixIdx > 0) {
                String prefix = s.substring(0, prefixIdx);
                Function<String, BlockMatcher> factory = api.getMatcherFactory(prefix);
                if (factory != null) {
                    return factory.apply(s.substring(prefixIdx + 1));
                }
            }

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
