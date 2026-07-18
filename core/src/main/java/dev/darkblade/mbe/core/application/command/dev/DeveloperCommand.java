package dev.darkblade.mbe.core.application.command.dev;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.command.addon.AddonsCommandRouter;
import dev.darkblade.mbe.core.application.command.service.ServicesCommandRouter;
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
    
    private final AddonsCommandRouter addonsRouter;
    private final ServicesCommandRouter servicesRouter;

    public DeveloperCommand(MultiBlockEngine plugin, PlayerMessageService messageService, DebugSessionService debugSessionService) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.debugSessionService = debugSessionService;
        
        this.addonsRouter = new AddonsCommandRouter(plugin);
        this.servicesRouter = new ServicesCommandRouter(plugin, plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.event.EventBusService.class));
        
        this.servicesRouter.registerInternal(new UiCommandService(plugin));
        this.servicesRouter.registerInternal(new ItemsCommandService(plugin));
        this.servicesRouter.registerInternal(new BlueprintCommandService(plugin));
        this.servicesRouter.registerInternal(new AssemblyCommandService(plugin));
    }

    @Command("mbe dev addons [args]")
    @Permission("multiblockengine.admin.addons")
    public void addons(
            CommandSender sender,
            @Argument("args") @Greedy @Default("") String argsStr
    ) {
        String[] rawArgs = argsStr.isEmpty() ? new String[]{"addons"} : ("addons " + argsStr).split(" ");
        addonsRouter.handle(sender, "mbe", rawArgs);
    }


    @Command("mbe dev services [args]")
    @Permission("multiblockengine.admin.services")
    public void services(
            CommandSender sender,
            @Argument("args") @Greedy @Default("") String argsStr
    ) {
        String[] rawArgs = argsStr.isEmpty() ? new String[]{"services"} : ("services " + argsStr).split(" ");
        servicesRouter.handle(sender, "mbe", rawArgs);
    }

    @Command("mbe dev ui panels")
    @Permission("multiblockengine.ui.debug")
    public void uiPanels(CommandSender sender) {
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
            Player player,
            @Argument("type") MultiblockType type,
            @Argument("target") Player targetArg
    ) {
        Player target = targetArg != null ? targetArg : player;
        MultiblockRuntimeService runtimeService = plugin.getManager();

        runtimeService.getSource(type.id()).ifPresent(src -> {
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
