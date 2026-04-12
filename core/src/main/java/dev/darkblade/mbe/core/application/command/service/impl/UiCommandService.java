package dev.darkblade.mbe.core.application.command.service.impl;

import dev.darkblade.mbe.api.command.MbeCommandService;
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
import dev.darkblade.mbe.core.application.service.ui.PanelViewResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class UiCommandService implements MbeCommandService {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_INFO_TITLE = MessageKey.of(ORIGIN, "services.ui.info.title");
    private static final MessageKey MSG_INFO_DESCRIPTION = MessageKey.of(ORIGIN, "services.ui.info.description");
    private static final MessageKey MSG_INFO_PANEL_SERVICE = MessageKey.of(ORIGIN, "services.ui.info.panel_service");
    private static final MessageKey MSG_INFO_BINDINGS_COUNT = MessageKey.of(ORIGIN, "services.ui.info.bindings_count");
    private static final MessageKey MSG_ERROR_UNKNOWN_SUBCOMMAND = MessageKey.of(ORIGIN, "services.ui.error.unknown_subcommand");
    private static final MessageKey MSG_ERROR_PLAYER_ONLY_LINK = MessageKey.of(ORIGIN, "services.ui.error.player_only_link");
    private static final MessageKey MSG_ERROR_SERVICES_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui.error.services_unavailable");
    private static final MessageKey MSG_USAGE_LINK = MessageKey.of(ORIGIN, "services.ui.usage.link");
    private static final MessageKey MSG_USAGE_CANCEL = MessageKey.of(ORIGIN, "services.ui.usage.cancel");
    private static final MessageKey MSG_USAGE_LIST = MessageKey.of(ORIGIN, "services.ui.usage.list");
    private static final MessageKey MSG_ERROR_RESOLVE_PANEL_SERVICE = MessageKey.of(ORIGIN, "services.ui.error.resolve_panel_service");
    private static final MessageKey MSG_ERROR_PANEL_SERVICE_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui.error.panel_service_unavailable");
    private static final MessageKey MSG_ERROR_PANEL_NOT_FOUND = MessageKey.of(ORIGIN, "services.ui.error.panel_not_found");
    private static final MessageKey MSG_ERROR_PANEL_VALIDATION_FAILED = MessageKey.of(ORIGIN, "services.ui.error.panel_validation_failed");
    private static final MessageKey MSG_LINK_STARTED = MessageKey.of(ORIGIN, "services.ui.link.started");
    private static final MessageKey MSG_LINK_CANCEL_HINT = MessageKey.of(ORIGIN, "services.ui.link.cancel_hint");
    private static final MessageKey MSG_ERROR_PLAYER_ONLY_CANCEL = MessageKey.of(ORIGIN, "services.ui.error.player_only_cancel");
    private static final MessageKey MSG_ERROR_EDITOR_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui.error.editor_unavailable");
    private static final MessageKey MSG_ERROR_BINDING_UNAVAILABLE = MessageKey.of(ORIGIN, "services.ui.error.binding_unavailable");
    private static final MessageKey MSG_LIST_TITLE = MessageKey.of(ORIGIN, "services.ui.list.title");
    private static final MessageKey MSG_LIST_ENTRY = MessageKey.of(ORIGIN, "services.ui.list.entry");

    private final EditorSessionManager sessions;
    private final PanelBindingService bindings;
    private final MultiblockRuntimeService multiblocks;
    private final I18nService i18n;
    private final PlayerMessageService messageService;

    public UiCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.sessions = plugin.getAddonLifecycleService().getCoreService(EditorSessionManager.class);
        this.bindings = plugin.getAddonLifecycleService().getCoreService(PanelBindingService.class);
        this.multiblocks = plugin.getManager();
        this.i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        this.messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }

    @Override
    public String id() {
        return "ui";
    }

    @Override
    public String description() {
        return "Binding entre paneles UI y controllers de multiblocks";
    }

    @Override
    public List<String> infoUsage() {
        return List.of("/mbe services call ui info");
    }

    @Override
    public List<String> executeUsage() {
        return List.of(
                "/mbe services call ui execute link <panelId>",
                "/mbe services call ui execute cancel",
                "/mbe services call ui execute list"
        );
    }

    @Override
    public void info(CommandSender sender, List<String> args) {
        send(sender, MSG_INFO_TITLE);
        send(sender, MSG_INFO_DESCRIPTION);
        send(sender, MSG_INFO_PANEL_SERVICE, java.util.Map.of("status", resolvePanelService() != null ? "available" : "unavailable"));
        send(sender, MSG_INFO_BINDINGS_COUNT, java.util.Map.of("count", bindings == null ? 0 : bindings.all().size()));
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        if (args == null || args.isEmpty()) {
            send(sender, MSG_USAGE_LINK);
            send(sender, MSG_USAGE_CANCEL);
            send(sender, MSG_USAGE_LIST);
            return;
        }
        String action = args.get(0).toLowerCase(Locale.ROOT);
        if (action.equals("link")) {
            executeLink(sender, args);
            return;
        }
        if (action.equals("cancel")) {
            executeCancel(sender);
            return;
        }
        if (action.equals("list")) {
            executeList(sender);
            return;
        }
        send(sender, MSG_ERROR_UNKNOWN_SUBCOMMAND, java.util.Map.of("action", action));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
        if (!"execute".equalsIgnoreCase(mode)) {
            return List.of();
        }
        if (args == null || args.isEmpty()) {
            return List.of("link", "cancel", "list");
        }
        if (args.size() == 1) {
            String typed = args.get(0).toLowerCase(Locale.ROOT);
            return List.of("link", "cancel", "list").stream().filter(v -> v.startsWith(typed)).toList();
        }
        return List.of();
    }

    private void executeLink(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player player)) {
            send(sender, MSG_ERROR_PLAYER_ONLY_LINK);
            return;
        }
        if (sessions == null || bindings == null) {
            send(sender, MSG_ERROR_SERVICES_UNAVAILABLE);
            return;
        }
        if (args.size() < 2) {
            send(sender, MSG_USAGE_LINK);
            return;
        }
        PanelViewService panelService;
        try {
            panelService = resolvePanelService();
        } catch (Throwable t) {
            send(sender, MSG_ERROR_RESOLVE_PANEL_SERVICE);
            return;
        }
        if (panelService == null) {
            send(sender, MSG_ERROR_PANEL_SERVICE_UNAVAILABLE);
            return;
        }
        String panelId = args.get(1);
        try {
            if (!panelService.panelExists(panelId)) {
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

    private void executeCancel(CommandSender sender) {
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

    private void executeList(CommandSender sender) {
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

    private PanelViewService resolvePanelService() {
        return PanelViewResolver.resolve();
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
