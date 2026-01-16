package com.darkbladedev.engine.export;

import com.darkbladedev.engine.api.export.ExportHook;
import com.darkbladedev.engine.api.export.ExportHookRegistry;

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

