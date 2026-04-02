package dev.darkblade.mbe.api.assembly;

import java.util.List;

public record AssemblyReport(
        String trigger,
        boolean triggerMatched,
        boolean controllerFound,
        MatcherResult matcherResult,
        List<ConditionResult> conditions,
        Result result,
        String multiblockId,
        String failureReason
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
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        trigger = trigger == null ? "" : trigger;
        multiblockId = multiblockId == null ? "" : multiblockId;
        failureReason = failureReason == null ? "" : failureReason;
    }
}

