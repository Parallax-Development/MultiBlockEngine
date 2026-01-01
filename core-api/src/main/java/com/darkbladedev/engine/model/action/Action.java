package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;

public interface Action {
    default String ownerId() {
        return null;
    }

    default String typeKey() {
        return null;
    }

    default void execute(MultiblockInstance instance) {
        execute(instance, null);
    }
    
    default void execute(MultiblockInstance instance, Player player) {
        execute(instance);
    }

    static Action owned(String ownerId, String typeKey, Action delegate) {
        return new Action() {
            @Override
            public String ownerId() {
                return ownerId;
            }

            @Override
            public String typeKey() {
                return typeKey;
            }

            @Override
            public void execute(MultiblockInstance instance, Player player) {
                delegate.execute(instance, player);
            }

            @Override
            public void execute(MultiblockInstance instance) {
                delegate.execute(instance);
            }
        };
    }
}
