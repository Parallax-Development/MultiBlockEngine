package dev.darkblade.mbe.core.domain.assembly;

import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyTrigger;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerRegistry;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerType;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import dev.darkblade.mbe.core.domain.assembly.trigger.TileEntityInteractTrigger;

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
            public boolean supports(InteractionIntent intent) {
                return intent != null && intent.type() == InteractionType.WRENCH_USE;
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                return context != null && context.intent() != null && context.intent().type() == InteractionType.WRENCH_USE;
            }
        });

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.SNEAK_RIGHT_CLICK.id();
            }

            @Override
            public boolean supports(InteractionIntent intent) {
                return intent != null && intent.type() == InteractionType.SHIFT_RIGHT_CLICK;
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                return context != null && context.intent() != null && context.intent().type() == InteractionType.SHIFT_RIGHT_CLICK;
            }
        });

        registry.register(new TileEntityInteractTrigger());

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.ON_FINAL_BLOCK_PLACED.id();
            }

            @Override
            public boolean supports(InteractionIntent intent) {
                return intent == null;
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                return context != null && context.intent() == null;
            }
        });

        registry.register(new AssemblyTrigger() {
            @Override
            public String id() {
                return AssemblyTriggerType.MANUAL_ONLY.id();
            }

            @Override
            public boolean supports(InteractionIntent intent) {
                return intent != null && intent.type() == InteractionType.PROGRAMMATIC;
            }

            @Override
            public boolean shouldTrigger(AssemblyContext context) {
                return context != null && context.intent() != null && context.intent().type() == InteractionType.PROGRAMMATIC;
            }
        });
    }
}
