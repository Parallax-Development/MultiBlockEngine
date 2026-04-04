package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.ui.binding.PanelBinding;

import java.util.Objects;

public final class ClickBindingTrigger implements BindingTrigger {
    private final InteractionRouter router;

    public ClickBindingTrigger(InteractionRouter router) {
        this.router = Objects.requireNonNull(router, "router");
    }

    @Override
    public String id() {
        return "click";
    }

    @Override
    public void register(PanelBinding binding) {
        router.registerClickBinding(binding);
    }

    @Override
    public void unregister(PanelBinding binding) {
        router.unregisterClickBinding(binding);
    }
}
