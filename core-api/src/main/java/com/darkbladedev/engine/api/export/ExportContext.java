package com.darkbladedev.engine.api.export;

import java.util.List;
import java.util.Map;

public interface ExportContext {
    void markRole(ExportBlockPos pos, String role);

    void putProperty(ExportBlockPos pos, String key, Object value);

    Map<ExportBlockPos, String> roles();

    Map<ExportBlockPos, Map<String, Object>> properties();

    void warn(String message);

    List<String> warnings();
}

