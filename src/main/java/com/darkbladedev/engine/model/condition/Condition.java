package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;

public interface Condition {
    boolean check(MultiblockInstance instance);
}
