package dev.darkblade.mbe.core.domain.assembly.pipeline;

public interface AssemblyStep {

    AssemblyStepResult execute(AssemblyPipelineContext context, AssemblyTraceCollector trace);
}
