package dev.darkblade.mbe.core.domain.assembly.pipeline;

import org.bukkit.block.Block;

import java.util.Map;

public final class ControllerMatchStep implements AssemblyStep {

    @Override
    public AssemblyStepResult execute(AssemblyPipelineContext ctx, AssemblyTraceCollector trace) {
        if (!ctx.triggerMatched()) {
            return AssemblyStepResult.continueStep();
        }
        Block controller = ctx.controller();
        boolean matched = controller != null && ctx.type() != null && ctx.type().controllerMatcher().matches(controller);
        trace.add(
                "controller_match",
                matched,
                matched ? "Controller matched" : "Controller mismatch",
                Map.of("multiblockId", ctx.multiblockId())
        );
        ctx.setControllerMatched(matched);
        return AssemblyStepResult.continueStep();
    }
}
