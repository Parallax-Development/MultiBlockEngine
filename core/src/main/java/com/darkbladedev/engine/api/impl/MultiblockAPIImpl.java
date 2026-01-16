package com.darkbladedev.engine.api.impl;

import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.model.condition.Condition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MultiblockAPIImpl implements MultiblockAPI {
    private final Map<String, Function<Map<String, Object>, Action>> actionRegistry = new HashMap<>();
    private final Map<String, Function<Map<String, Object>, Condition>> conditionRegistry = new HashMap<>();
    private final Map<String, Function<String, BlockMatcher>> matcherRegistry = new HashMap<>();

    @Override
    public void registerAction(String type, Function<Map<String, Object>, Action> factory) {
        actionRegistry.put(type.toLowerCase(), factory);
    }

    @Override
    public void registerCondition(String type, Function<Map<String, Object>, Condition> factory) {
        conditionRegistry.put(type.toLowerCase(), factory);
    }

    @Override
    public void registerMatcher(String prefix, Function<String, BlockMatcher> factory) {
        matcherRegistry.put(prefix.toLowerCase(), factory);
    }
    
    @Override
    public com.darkbladedev.engine.api.builder.MultiblockBuilder createMultiblock(String id) {
        return new com.darkbladedev.engine.api.builder.MultiblockBuilder(id);
    }
    
    @Override
    public void registerMultiblock(com.darkbladedev.engine.model.MultiblockType type) {
        com.darkbladedev.engine.MultiBlockEngine.getInstance().getManager().registerType(type);
    }
    
    public Function<Map<String, Object>, Action> getActionFactory(String type) {
        return actionRegistry.get(type.toLowerCase());
    }
    
    public Function<Map<String, Object>, Condition> getConditionFactory(String type) {
        return conditionRegistry.get(type.toLowerCase());
    }
    
    public Function<String, BlockMatcher> getMatcherFactory(String prefix) {
        return matcherRegistry.get(prefix.toLowerCase());
    }
}
