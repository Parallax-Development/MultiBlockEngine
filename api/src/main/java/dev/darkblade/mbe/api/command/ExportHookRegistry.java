package dev.darkblade.mbe.api.command;

import java.util.List;

public interface ExportHookRegistry {
    void register(ExportHook hook);

    List<ExportHook> hooks();
}

