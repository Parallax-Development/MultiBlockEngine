package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.api.assembly.AssemblyStepTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AssemblyTraceCollector {

    private final List<AssemblyStepTrace> trace = new ArrayList<>();

    public void add(String step, boolean success, String detail, Map<String, Object> data) {
        trace.add(new AssemblyStepTrace(step, success, detail, data));
    }

    public List<AssemblyStepTrace> getTrace() {
        return List.copyOf(trace);
    }
}
