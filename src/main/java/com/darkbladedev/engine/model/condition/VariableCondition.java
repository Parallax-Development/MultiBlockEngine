package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;

public record VariableCondition(String variable, Object expectedValue) implements Condition {
    @Override
    public boolean check(MultiblockInstance instance) {
        Object val = instance.getVariable(variable);
        if (val == null) return expectedValue == null;
        
        // Handle number conversions (Double vs Integer from GSON/YAML)
        if (val instanceof Number n1 && expectedValue instanceof Number n2) {
            return n1.doubleValue() == n2.doubleValue();
        }
        
        return val.equals(expectedValue);
    }
}
