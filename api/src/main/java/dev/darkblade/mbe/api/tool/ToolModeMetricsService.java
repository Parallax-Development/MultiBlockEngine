package dev.darkblade.mbe.api.tool;

import java.util.Map;

public interface ToolModeMetricsService {
    void recordAttempt(String modeId);

    void recordSuccess(String modeId);

    void recordFailure(String modeId);

    Map<String, Long> attempts();

    Map<String, Long> successes();

    Map<String, Long> failures();
}
