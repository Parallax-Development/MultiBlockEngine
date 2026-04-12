package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;

final class PlayerMessageServiceLocator {
    private static volatile PlayerMessageService override;

    private PlayerMessageServiceLocator() {
    }

    static PlayerMessageService resolve() {
        PlayerMessageService local = override;
        if (local != null) {
            return local;
        }
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }

    static void override(PlayerMessageService service) {
        override = service;
    }
}
