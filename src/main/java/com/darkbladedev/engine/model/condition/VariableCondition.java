package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;

public record VariableCondition(String variable, Object expectedValue, Comparison comparison) implements Condition {
    
    public enum Comparison {
        EQUALS, NOT_EQUALS, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL
    }

    @Override
    public boolean check(MultiblockInstance instance) {
        Object val = instance.getVariable(variable);
        if (val == null) return expectedValue == null && comparison == Comparison.EQUALS;
        
        // Handle number conversions
        if (val instanceof Number n1 && expectedValue instanceof Number n2) {
            double d1 = n1.doubleValue();
            double d2 = n2.doubleValue();
            
            return switch (comparison) {
                case EQUALS -> d1 == d2;
                case NOT_EQUALS -> d1 != d2;
                case GREATER -> d1 > d2;
                case LESS -> d1 < d2;
                case GREATER_OR_EQUAL -> d1 >= d2;
                case LESS_OR_EQUAL -> d1 <= d2;
            };
        }
        
        // Fallback for non-numbers (only equality/inequality)
        return switch (comparison) {
            case EQUALS -> val.equals(expectedValue);
            case NOT_EQUALS -> !val.equals(expectedValue);
            default -> false;
        };
    }
}
