package com.darkbladedev.engine.model.condition;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;

public interface Condition {
    default String ownerId() {
        return null;
    }

    default String typeKey() {
        return null;
    }

    // Default method for backward compatibility
    default boolean check(MultiblockInstance instance) {
        return check(instance, null);
    }

    // New method supporting player context
    default boolean check(MultiblockInstance instance, Player player) {
        return check(instance);
    }

    static Condition owned(String ownerId, String typeKey, Condition delegate) {
        return new Condition() {
            @Override
            public String ownerId() {
                return ownerId;
            }

            @Override
            public String typeKey() {
                return typeKey;
            }

            @Override
            public boolean check(MultiblockInstance instance, Player player) {
                return delegate.check(instance, player);
            }

            @Override
            public boolean check(MultiblockInstance instance) {
                return delegate.check(instance);
            }
        };
    }
}
