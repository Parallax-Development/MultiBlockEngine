package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.ui.binding.PanelBinding;

public interface BindingTrigger {
    String id();
    void register(PanelBinding binding);
    void unregister(PanelBinding binding);
}
