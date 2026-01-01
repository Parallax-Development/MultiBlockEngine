package com.darkbladedev.engine.api;

import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.model.condition.Condition;

import java.util.Map;
import java.util.function.Function;

public interface MultiblockAPI {
    void registerAction(String type, Function<Map<String, Object>, Action> factory);
    void registerCondition(String type, Function<Map<String, Object>, Condition> factory);
    void registerMatcher(String prefix, Function<String, BlockMatcher> factory); // For things like "TAG:minecraft:logs"
    
    com.darkbladedev.engine.api.builder.MultiblockBuilder createMultiblock(String id);
    void registerMultiblock(com.darkbladedev.engine.model.MultiblockType type);
}
