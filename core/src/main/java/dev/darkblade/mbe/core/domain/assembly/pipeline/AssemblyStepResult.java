package dev.darkblade.mbe.core.domain.assembly.pipeline;

import java.util.Map;

public record AssemblyStepResult(
        AssemblyStepResultType type,
        String reasonKey,
        Map<String, Object> data
) {

    public AssemblyStepResult {
        type = type == null ? AssemblyStepResultType.CONTINUE : type;
        reasonKey = reasonKey == null || reasonKey.isBlank() ? null : reasonKey;
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static AssemblyStepResult continueStep() {
        return new AssemblyStepResult(AssemblyStepResultType.CONTINUE, null, Map.of());
    }

    public static AssemblyStepResult fail(String reasonKey, Map<String, Object> data) {
        return new AssemblyStepResult(AssemblyStepResultType.FAIL, reasonKey, data);
    }

    public static AssemblyStepResult success() {
        return new AssemblyStepResult(AssemblyStepResultType.SUCCESS, null, Map.of());
    }
}
