package dev.darkblade.mbe.core.application.command.service.impl;

import dev.darkblade.mbe.api.blueprint.BlueprintService;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

public final class BlueprintCommandService {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_SERVICE_UNAVAILABLE = MessageKey.of(ORIGIN, "services.blueprint.error.service_unavailable");
    private static final MessageKey MSG_INFO_EXECUTE_CATALOG = MessageKey.of(ORIGIN, "services.blueprint.info.execute_catalog");
    private static final MessageKey MSG_PLAYER_ONLY = MessageKey.of(ORIGIN, "services.blueprint.error.player_only");
    private static final MessageKey MSG_USAGE_CATALOG = MessageKey.of(ORIGIN, "services.blueprint.usage.catalog");
    private static final MessageKey MSG_INVALID_SUBCOMMAND = MessageKey.of(ORIGIN, "services.blueprint.error.invalid_subcommand");

    private final BlueprintService blueprintService;
    private final I18nService i18n;
    private final PlayerMessageService messageService;

    public BlueprintCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.blueprintService = plugin.getAddonLifecycleService().getCoreService(BlueprintService.class);
        this.i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        this.messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }

    public void openCatalog(Player player) {
        if (player == null) {
            return;
        }
        if (blueprintService == null) {
            send(player, MSG_SERVICE_UNAVAILABLE);
            return;
        }
        blueprintService.openCatalog(player);
    }



    @Command("mbe dev debug blueprint info")
    @Permission("multiblockengine.admin.services")
    public void info(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        send(sender, MSG_INFO_EXECUTE_CATALOG);
    }

    @Command("mbe dev debug blueprint catalog")
    @Permission("multiblockengine.admin.services")
    public void executeCatalog(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) {
            send(sender, MSG_PLAYER_ONLY);
            return;
        }
        openCatalog(player);
    }

    private void send(CommandSender sender, MessageKey key) {
        if (sender == null || key == null) {
            return;
        }
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, java.util.Map.of()));
            return;
        }
        if (i18n != null) {
            i18n.send(sender, key);
        }
    }
}
