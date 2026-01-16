package com.darkbladedev.engine.assembly;

import com.darkbladedev.engine.api.assembly.AssemblyContext;
import com.darkbladedev.engine.api.assembly.AssemblyTrigger;
import com.darkbladedev.engine.api.assembly.AssemblyTriggerRegistry;
import com.darkbladedev.engine.api.assembly.AssemblyTriggerType;

import java.util.Objects;

public final class BuiltinAssemblyTriggers {

    private BuiltinAssemblyTriggers() {
    }

    public static void registerAll(AssemblyTriggerRegistry registry) {
        Objects.requireNonNull(registry, "registry");

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.WRENCH_USE.id();
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                if (context == null) {
                    return false;
                }
                return context.cause() == AssemblyContext.Cause.PLAYER_INTERACT
                        && context.action() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                        && context.attributeBoolean("wrench");
            }
        });

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.SNEAK_RIGHT_CLICK.id();
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                if (context == null) {
                    return false;
                }
                return context.cause() == AssemblyContext.Cause.PLAYER_INTERACT
                        && context.action() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                        && context.sneaking();
            }
        });

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.ON_FINAL_BLOCK_PLACED.id();
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                return context != null && context.cause() == AssemblyContext.Cause.BLOCK_PLACE;
            }
        });

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.MANUAL_ONLY.id();
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                return context != null && context.cause() == AssemblyContext.Cause.MANUAL;
            }
        });
    }
}

