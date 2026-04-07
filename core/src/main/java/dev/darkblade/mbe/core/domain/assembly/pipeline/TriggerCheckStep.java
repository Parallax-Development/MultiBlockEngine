package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.api.assembly.AssemblyTrigger;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerRegistry;

import java.util.Map;
import java.util.Objects;

public final class TriggerCheckStep implements AssemblyStep {

    private final AssemblyTriggerRegistry triggerRegistry;

    public TriggerCheckStep(AssemblyTriggerRegistry triggerRegistry) {
        this.triggerRegistry = Objects.requireNonNull(triggerRegistry, "triggerRegistry");
    }

    @Override
    public AssemblyStepResult execute(AssemblyPipelineContext ctx, AssemblyTraceCollector trace) {
        String triggerId = ctx.triggerId();
        if (ctx.forceTrigger()) {
            ctx.setTriggerMatched(true);
            trace.add("trigger_check", true, "Trigger forced", Map.of("trigger", triggerId, "multiblockId", ctx.multiblockId()));
            return AssemblyStepResult.continueStep();
        }
        AssemblyTrigger trigger = triggerRegistry.get(triggerId).orElse(null);
        if (trigger == null) {
            trace.add("trigger_check", false, "Unknown trigger", Map.of("trigger", triggerId, "multiblockId", ctx.multiblockId()));
            return AssemblyStepResult.continueStep();
        }
        if (!trigger.supports(ctx.assemblyContext() == null ? null : ctx.assemblyContext().intent())) {
            trace.add("trigger_check", false, "Trigger not supported", Map.of("trigger", triggerId, "multiblockId", ctx.multiblockId()));
            return AssemblyStepResult.continueStep();
        }
        boolean shouldTrigger = trigger.shouldTrigger(ctx.assemblyContext());
        trace.add(
                "trigger_check",
                shouldTrigger,
                shouldTrigger ? "Trigger executed" : "Trigger not matched",
                Map.of("trigger", triggerId, "multiblockId", ctx.multiblockId())
        );
        ctx.setTriggerMatched(shouldTrigger);
        return AssemblyStepResult.continueStep();
    }
}
