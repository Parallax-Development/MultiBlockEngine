package dev.darkblade.mbe.core.internal.tooling.export;

import dev.darkblade.mbe.api.command.ExportHook;
import dev.darkblade.mbe.api.command.ExportHookRegistry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultExportHookRegistry implements ExportHookRegistry {

    private final CopyOnWriteArrayList<ExportHook> hooks = new CopyOnWriteArrayList<>();

    @Override
    public void register(ExportHook hook) {
        if (hook == null) {
            return;
        }
        hooks.addIfAbsent(hook);
    }

    @Override
    public List<ExportHook> hooks() {
        return List.copyOf(hooks);
    }
}

