package com.darkbladedev.engine.api.export;

public interface ExportHook {
    void onBlockExport(ExportBlockSnapshot block, ExportContext context);
}

