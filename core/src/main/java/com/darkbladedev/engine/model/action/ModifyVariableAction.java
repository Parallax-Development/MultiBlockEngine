package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;

public record ModifyVariableAction(String key, double amount, Operation operation) implements Action {
    public enum Operation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }

    @Override
    public void execute(MultiblockInstance instance) {
        Object current = instance.getVariable(key);
        double val = 0;
        if (current instanceof Number n) {
            val = n.doubleValue();
        }
        
        double newVal = switch (operation) {
            case ADD -> val + amount;
            case SUBTRACT -> val - amount;
            case MULTIPLY -> val * amount;
            case DIVIDE -> amount != 0 ? val / amount : val;
        };
        
        // Store as Integer if it was an Integer/whole number to keep it clean, else Double
        if (newVal == (long) newVal) {
            instance.setVariable(key, (long) newVal);
        } else {
            instance.setVariable(key, newVal);
        }
    }
}
