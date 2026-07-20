package dev.darkblade.mbe.core.application.command.dev;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;


import dev.darkblade.mbe.core.application.command.service.impl.AssemblyCommandService;
import dev.darkblade.mbe.core.application.command.service.impl.BlueprintCommandService;
import dev.darkblade.mbe.core.application.command.service.impl.ItemsCommandService;
import dev.darkblade.mbe.core.application.command.service.impl.UiCommandService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.internal.debug.DebugSessionService;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;

import java.util.List;
import java.util.Map;

public class DeveloperCommand {

    private static final String ORIGIN = "mbe";

    private final MultiBlockEngine plugin;
    private final PlayerMessageService messageService;
    private final DebugSessionService debugSessionService;

    public DeveloperCommand(MultiBlockEngine plugin, PlayerMessageService messageService, DebugSessionService debugSessionService) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.debugSessionService = debugSessionService;
    }

    @Command("mbe dev addons list")
    @Permission("multiblockengine.admin.addons")
    public void addonsList(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        List<dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo> addons = plugin.getAddonLifecycleService().getAddonInfoList();
        if (addons.isEmpty()) {
            sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.list.none"), Map.of());
            return;
        }
        sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.list.title"), Map.of("count", addons.size()));
        for (dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo info : addons) {
            sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.list.entry"), Map.of(
                    "id", info.id(),
                    "version", info.version() == null ? "" : info.version(),
                    "state", coloredState(info.state())
            ));
        }
    }

    @org.incendo.cloud.annotations.suggestion.Suggestions("addonIds")
    public List<String> suggestAddonIds(org.incendo.cloud.context.CommandContext<dev.darkblade.mbe.core.application.command.MBESender> context, String input) {
        return plugin.getAddonLifecycleService().getAddonInfoList().stream()
                .map(dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo::id)
                .toList();
    }

    @Command("mbe dev addons status <addonId>")
    @Permission("multiblockengine.admin.addons")
    public void addonsStatus(
            dev.darkblade.mbe.core.application.command.MBESender mbeSender,
            @Argument(value = "addonId", suggestions = "addonIds") String addonId
    ) {
        CommandSender sender = mbeSender.getSender();
        java.util.Optional<dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo> opt = plugin.getAddonLifecycleService().getAddonInfo(addonId);
        if (opt.isEmpty()) {
            sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.not_found"), Map.of("id", addonId));
            return;
        }
        dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo info = opt.get();
        sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.title"), Map.of("id", info.id()));
        sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.version"), Map.of("version", info.version() == null ? "" : info.version()));
        sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.state"), Map.of("state", coloredState(info.state())));
        String deps = info.dependencies().isEmpty() ? "none" : String.join(", ", info.dependencies());
        sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.dependencies"), Map.of("deps", deps));
        if (info.serviceIds().isEmpty()) {
            sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.no_services"), Map.of());
        } else {
            sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.services_title"), Map.of("count", info.serviceIds().size()));
            for (String svc : info.serviceIds()) {
                sendMessage(sender, MessageKey.of(ORIGIN, "addons.router.status.service_entry"), Map.of("serviceId", svc));
            }
        }
    }

    private static String coloredState(dev.darkblade.mbe.core.application.service.addon.domain.AddonState state) {
        if (state == null) {
            return "&7UNKNOWN";
        }
        return switch (state) {
            case ENABLED -> "&a" + state.name();
            case FAILED -> "&c" + state.name();
            case DISABLED -> "&e" + state.name();
            case LOADED -> "&7" + state.name();
            case DISCOVERED -> "&7" + state.name();
        };
    }




    @org.incendo.cloud.annotations.suggestion.Suggestions("panelIds")
    public List<String> suggestPanelIds(org.incendo.cloud.context.CommandContext<dev.darkblade.mbe.core.application.command.MBESender> context, String input) {
        dev.darkblade.mbe.api.ui.PanelViewService panelViewService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.ui.PanelViewService.class);
        if (panelViewService == null) return List.of();
        return panelViewService.getRegisteredPanelIds();
    }

    @Command("mbe dev ui panels")
    @Permission("multiblockengine.ui.debug")
    public void uiPanels(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        dev.darkblade.mbe.api.ui.PanelViewService panelViewService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.ui.PanelViewService.class);
        if (panelViewService == null) {
            sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.unavailable"), Map.of());
            return;
        }
        List<String> panelIds = panelViewService.getRegisteredPanelIds();
        sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.title"), MessageUtils.params("count", panelIds.size()));
        if (panelIds.isEmpty()) {
            sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.none"), Map.of());
            return;
        }
        for (String panelId : panelIds) {
            sendMessage(sender, MessageKey.of(ORIGIN, "commands.ui.debug.entry"), MessageUtils.params("panel", panelId));
        }
    }

    @Command("mbe dev type <type> [target]")
    @Permission("multiblockengine.debug.session")
    public void debugType(
            dev.darkblade.mbe.core.application.command.MBESender mbeSender,
            @Argument("type") MultiblockType type,
            @Argument("target") Player targetArg
    ) {
        Player player = mbeSender.getPlayer();
        Player target = targetArg != null ? targetArg : player;
        MultiblockRuntimeService runtimeService = plugin.getManager();

        runtimeService.getSource(type.id().toString()).ifPresent(src -> {
            sendMessage(player, CoreMessageKeys.DEBUG_SOURCE, MessageUtils.params("sourceType", src.type().name(), "path", src.path()));
        });
        sendMessage(player, CoreMessageKeys.DEBUG_SIGNATURE, MessageUtils.params("signature", runtimeService.signatureOf(type)));

        Block targetBlock = target.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            sendMessage(player, MessageKey.of("mbe", "commands.debug.must_look_at_block"), Map.of());
            return;
        }

        debugSessionService.startSession(target, type, targetBlock.getLocation());
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            dev.darkblade.mbe.api.i18n.I18nService i18n = null;
            if (plugin != null && plugin.getAddonLifecycleService() != null) {
                i18n = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.i18n.I18nService.class);
            }
            if (i18n != null) {
                i18n.send(sender, key, params);
            } else {
                plugin.getLogger().info(key.fullKey() + " " + params.toString());
            }
        }
    }
}
