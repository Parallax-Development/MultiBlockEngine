package com.darkbladedev.engine.api.export;

import java.util.List;

public interface ExportHookRegistry {
    void register(ExportHook hook);

    List<ExportHook> hooks();
}

