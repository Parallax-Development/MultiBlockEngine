package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class InstanceCreateStep implements AssemblyStep {

    private final MultiblockRuntimeService runtimeService;
    private final Supplier<MultiblockLimitService> limitServiceSupplier;

    public InstanceCreateStep(MultiblockRuntimeService runtimeService, Supplier<MultiblockLimitService> limitServiceSupplier) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
        this.limitServiceSupplier = limitServiceSupplier;
    }

    @Override
    public AssemblyStepResult execute(AssemblyPipelineContext ctx, AssemblyTraceCollector trace) {
        if (!ctx.isMatched()) {
            return AssemblyStepResult.continueStep();
        }
        var created = runtimeService.tryCreate(ctx.controller(), ctx.type(), ctx.player());
        if (created.isPresent()) {
            MultiblockLimitService limitService = limitServiceSupplier == null ? null : limitServiceSupplier.get();
            if (limitService != null && ctx.player() != null) {
                limitService.registerAssembly(ctx.player(), ctx.multiblockId());
            }
            trace.add("instance_create", true, "Instance created", Map.of("multiblockId", ctx.multiblockId()));
            return AssemblyStepResult.success();
        }
        trace.add("instance_create", false, "Creation cancelled", Map.of("multiblockId", ctx.multiblockId()));
        return AssemblyStepResult.fail("creation_cancelled", Map.of("multiblockId", ctx.multiblockId()));
    }
}
