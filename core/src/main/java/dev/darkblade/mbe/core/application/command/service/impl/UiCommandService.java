package dev.darkblade.mbe.core.application.command.service.impl;


import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.editor.EditorSessionManager;
import dev.darkblade.mbe.core.application.service.ui.LinkPanelSession;
import dev.darkblade.mbe.core.application.service.ui.PanelBindingService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

public final class UiCommandService {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_INFO_TITLE = MessageKey.of(ORIGIN, "services.ui_bindings.info.title");
    private static final MessageKey MSG_INFO_DESCRIPTION = MessageKey.of(ORIGIN, "services.ui_bindings.info.description");
    private static final MessageKey MSG_INFO_PANEL_SERVICE = MessageKey.of(ORIGIN, "services.ui_bindings.info.panel_service");
    private static final MessageKey MSG_INFO_BINDINGS_COUNT = MessageKey.of(ORIGIN, "services.ui_bindings.info.bindings_count");
    private static final MessageKey MSG_ERROR_UNKNOWN_SUBCOMMAND = MessageKey.of(ORIGIN, "services.ui_bindings.error.unknown_subcommand");
    private static final MessageKey MSG_ERROR_PLAYER_ONLY_LINK = MessageKey.of(ORIGIN, "services.ui_bindings.error.player_only_link");
    private static final MessageKey MSG_ERROR_SERVICES_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui_bindings.error.services_unavailable");
    private static final MessageKey MSG_USAGE_LINK = MessageKey.of(ORIGIN, "services.ui_bindings.usage.link");
    private static final MessageKey MSG_USAGE_CANCEL = MessageKey.of(ORIGIN, "services.ui_bindings.usage.cancel");
    private static final MessageKey MSG_USAGE_LIST = MessageKey.of(ORIGIN, "services.ui_bindings.usage.list");
    private static final MessageKey MSG_ERROR_PANEL_SERVICE_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui_bindings.error.panel_service_unavailable");
    private static final MessageKey MSG_ERROR_PANEL_NOT_FOUND = MessageKey.of(ORIGIN, "services.ui_bindings.error.panel_not_found");
    private static final MessageKey MSG_ERROR_PANEL_VALIDATION_FAILED = MessageKey.of(ORIGIN, "services.ui_bindings.error.panel_validation_failed");
    private static final MessageKey MSG_LINK_STARTED = MessageKey.of(ORIGIN, "services.ui_bindings.link.started");
    private static final MessageKey MSG_LINK_CANCEL_HINT = MessageKey.of(ORIGIN, "services.ui_bindings.link.cancel_hint");
    private static final MessageKey MSG_ERROR_PLAYER_ONLY_CANCEL = MessageKey.of(ORIGIN, "services.ui_bindings.error.player_only_cancel");
    private static final MessageKey MSG_ERROR_EDITOR_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui_bindings.error.editor_unavailable");
    private static final MessageKey MSG_ERROR_BINDING_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui_bindings.error.binding_unavailable");
    private static final MessageKey MSG_LIST_TITLE = MessageKey.of(ORIGIN, "services.ui_bindings.list.title");
    private static final MessageKey MSG_LIST_ENTRY = MessageKey.of(ORIGIN, "services.ui_bindings.list.entry");

    private final EditorSessionManager sessions;
    private final PanelBindingService bindings;
    private final MultiblockRuntimeService multiblocks;
    private final PanelViewService panelViewService;
    private final I18nService i18n;
    private final PlayerMessageService messageService;

    public UiCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.sessions = plugin.getAddonLifecycleService().getCoreService(EditorSessionManager.class);
        this.bindings = plugin.getAddonLifecycleService().getCoreService(PanelBindingService.class);
        this.multiblocks = plugin.getManager();
        this.panelViewService = plugin.getAddonLifecycleService().getCoreService(PanelViewService.class);
        this.i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        this.messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }



    @Command("mbe dev services ui info")
    @Permission("multiblockengine.admin.services")
    public void info(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        send(sender, MSG_INFO_TITLE);
        send(sender, MSG_INFO_DESCRIPTION);
        send(sender, MSG_INFO_PANEL_SERVICE, java.util.Map.of("status", panelViewService != null ? "available" : "unavailable"));
        send(sender, MSG_INFO_BINDINGS_COUNT, java.util.Map.of("count", bindings == null ? 0 : bindings.all().size()));
    }

    @Command("mbe dev services ui link <panelId>")
    @Permission("multiblockengine.admin.services")
    public void executeLink(dev.darkblade.mbe.core.application.command.MBESender mbeSender, @Argument(value = "panelId", suggestions = "panelIds") String panelId) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) {
            send(sender, MSG_ERROR_PLAYER_ONLY_LINK);
            return;
        }
        if (sessions == null || bindings == null) {
            send(sender, MSG_ERROR_SERVICES_UNAVAILABLE);
            return;
        }
        if (panelViewService == null) {
            send(sender, MSG_ERROR_PANEL_SERVICE_UNAVAILABLE);
            return;
        }
        try {
            if (panelViewService.getPanel(panelId).isEmpty()) {
                send(sender, MSG_ERROR_PANEL_NOT_FOUND, java.util.Map.of("panel", panelId));
                return;
            }
        } catch (Throwable t) {
            send(sender, MSG_ERROR_PANEL_VALIDATION_FAILED, java.util.Map.of("panel", panelId));
            return;
        }
        LinkPanelSession session = new LinkPanelSession(player.getUniqueId(), panelId, sessions, bindings, multiblocks);
        sessions.startSession(player, session);
        send(sender, MSG_LINK_STARTED);
        send(sender, MSG_LINK_CANCEL_HINT);
    }

    @Command("mbe dev services ui cancel")
    @Permission("multiblockengine.admin.services")
    public void executeCancel(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) {
            send(sender, MSG_ERROR_PLAYER_ONLY_CANCEL);
            return;
        }
        if (sessions == null) {
            send(sender, MSG_ERROR_EDITOR_UNAVAILABLE);
            return;
        }
        sessions.cancelSession(player.getUniqueId());
    }

    @Command("mbe dev services ui list")
    @Permission("multiblockengine.admin.services")
    public void executeList(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (bindings == null) {
            send(sender, MSG_ERROR_BINDING_UNAVAILABLE);
            return;
        }
        send(sender, MSG_LIST_TITLE, java.util.Map.of("count", bindings.all().size()));
        bindings.all().stream().limit(20).forEach(binding ->
                send(sender, MSG_LIST_ENTRY, java.util.Map.of(
                        "panel", binding.panelId(),
                        "world", binding.world(),
                        "x", binding.x(),
                        "y", binding.y(),
                        "z", binding.z(),
                        "trigger", binding.triggerType()
                )));
    }

    private void send(CommandSender sender, MessageKey key) {
        send(sender, key, java.util.Map.of());
    }

    private void send(CommandSender sender, MessageKey key, java.util.Map<String, Object> params) {
        if (sender == null || key == null) {
            return;
        }
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, params == null ? java.util.Map.of() : params));
            return;
        }
        if (i18n != null) {
            i18n.send(sender, key, params == null ? java.util.Map.of() : params);
        }
    }

}
