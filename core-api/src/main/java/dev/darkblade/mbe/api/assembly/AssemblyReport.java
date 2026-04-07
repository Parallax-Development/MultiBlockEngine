package dev.darkblade.mbe.api.assembly;

import java.util.List;
import java.util.Map;

public record AssemblyReport(
        boolean success,
        String reasonKey,
        Map<String, Object> debugData,
        List<AssemblyStepTrace> trace
) {

    public enum MatcherResult {
        MATCH,
        MISMATCH,
        SKIPPED
    }

    public enum Result {
        SUCCESS,
        FAILED,
        ABORTED
    }

    public enum ConditionStatus {
        OK,
        FAILED,
        SKIPPED
    }

    public record ConditionResult(String id, ConditionStatus status) {
    }

    public AssemblyReport {
        reasonKey = reasonKey == null || reasonKey.isBlank() ? null : reasonKey;
        debugData = debugData == null ? Map.of() : Map.copyOf(debugData);
        trace = trace == null ? List.of() : List.copyOf(trace);
    }

    public static AssemblyReport success(List<AssemblyStepTrace> trace) {
        return new AssemblyReport(true, null, Map.of(), trace);
    }

    public static AssemblyReport fail(
            String reasonKey,
            Map<String, Object> debugData,
            List<AssemblyStepTrace> trace
    ) {
        return new AssemblyReport(false, reasonKey, debugData, trace);
    }

    public Result result() {
        return success ? Result.SUCCESS : Result.FAILED;
    }

    public String failureReason() {
        return reasonKey == null ? "" : reasonKey;
    }

    public String trigger() {
        return trace.stream()
                .filter(step -> "trigger_check".equals(step.step()))
                .map(step -> step.data().get("trigger"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
    }

    public String multiblockId() {
        return trace.stream()
                .map(AssemblyStepTrace::data)
                .map(data -> data.get("multiblockId"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(id -> !id.isBlank())
                .findFirst()
                .orElse("");
    }

    public boolean triggerMatched() {
        return trace.stream()
                .filter(step -> "trigger_check".equals(step.step()))
                .findFirst()
                .map(AssemblyStepTrace::success)
                .orElse(false);
    }

    public boolean controllerFound() {
        return trace.stream()
                .filter(step -> "controller_check".equals(step.step()))
                .findFirst()
                .map(AssemblyStepTrace::success)
                .orElse(false);
    }

    public MatcherResult matcherResult() {
        return trace.stream()
                .filter(step -> "pattern_match".equals(step.step()))
                .findFirst()
                .map(step -> step.success() ? MatcherResult.MATCH : MatcherResult.MISMATCH)
                .orElse(MatcherResult.SKIPPED);
    }

    public List<ConditionResult> conditions() {
        return List.of();
    }
}
