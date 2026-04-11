package dev.darkblade.mbe.api.assembly;

import java.util.Map;

public record AssemblyStepTrace(
        String step,
        boolean success,
        String detail,
        Map<String, Object> data
) {
    public AssemblyStepTrace {
        step = step == null ? "" : step;
        detail = detail == null ? "" : detail;
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
