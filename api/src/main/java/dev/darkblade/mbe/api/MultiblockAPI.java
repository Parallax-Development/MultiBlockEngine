package dev.darkblade.mbe.api;

import dev.darkblade.mbe.core.domain.BlockMatcher;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.condition.Condition;

import java.util.Map;
import java.util.function.Function;

public interface MultiblockAPI {
    void registerAction(String type, Function<Map<String, Object>, Action> factory);
    void registerCondition(String type, Function<Map<String, Object>, Condition> factory);
    void registerMatcher(String prefix, Function<String, BlockMatcher> factory); // For things like "TAG:minecraft:logs"
    
    dev.darkblade.mbe.api.assembly.MultiblockBuilder createMultiblock(String id);
    void registerMultiblock(dev.darkblade.mbe.core.domain.MultiblockType type);
}
