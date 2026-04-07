package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Supplier;

public final class LimitCheckStep implements AssemblyStep {

    private final Supplier<MultiblockLimitService> limitServiceSupplier;

    public LimitCheckStep(Supplier<MultiblockLimitService> limitServiceSupplier) {
        this.limitServiceSupplier = limitServiceSupplier;
    }

    @Override
    public AssemblyStepResult execute(AssemblyPipelineContext ctx, AssemblyTraceCollector trace) {
        if (!ctx.isMatched()) {
            return AssemblyStepResult.continueStep();
        }
        Player player = ctx.player();
        MultiblockLimitService limitService = limitServiceSupplier == null ? null : limitServiceSupplier.get();
        if (player == null || limitService == null) {
            trace.add("limit_check", true, "Limit check skipped", Map.of("multiblockId", ctx.multiblockId()));
            return AssemblyStepResult.continueStep();
        }
        boolean allowed = limitService.canAssemble(player, ctx.multiblockId());
        int current = limitService.getCurrentCount(player, ctx.multiblockId());
        int max = limitService.getLimit(player, ctx.multiblockId());
        trace.add(
                "limit_check",
                allowed,
                allowed ? "Limit check passed" : "Limit exceeded",
                Map.of("current", current, "max", max, "multiblockId", ctx.multiblockId())
        );
        if (!allowed) {
            return AssemblyStepResult.fail("limit_reached", Map.of("current", current, "max", max, "multiblockId", ctx.multiblockId()));
        }
        return AssemblyStepResult.continueStep();
    }
}
