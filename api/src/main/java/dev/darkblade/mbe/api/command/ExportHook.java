package dev.darkblade.mbe.api.command;

public interface ExportHook {
    void onBlockExport(ExportBlockSnapshot block, ExportContext context);
}

