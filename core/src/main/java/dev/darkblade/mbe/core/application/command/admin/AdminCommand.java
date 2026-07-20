package dev.darkblade.mbe.core.application.command.admin;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MetricsService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.infrastructure.config.parser.MultiblockParser;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.assembly.AssemblyStepTrace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCommand {

    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_STATUS_TITLE = MessageKey.of(ORIGIN, "commands.status.title");
    private static final MessageKey MSG_STATUS_LOADED_TYPES_VALUE = MessageKey.of(ORIGIN, "commands.status.loaded_types");
    private static final MessageKey MSG_STATUS_TOTAL_CREATED_VALUE = MessageKey.of(ORIGIN, "commands.status.created_instances");
    private static final MessageKey MSG_STATUS_TOTAL_DESTROYED_VALUE = MessageKey.of(ORIGIN, "commands.status.destroyed_instances");
    private static final MessageKey MSG_STATUS_STRUCTURE_CHECKS_VALUE = MessageKey.of(ORIGIN, "commands.status.structure_checks");
    private static final MessageKey MSG_STATUS_AVG_TICK_VALUE = MessageKey.of(ORIGIN, "commands.status.avg_tick_time");

    private static final MessageKey MSG_RELOAD_START = MessageKey.of(ORIGIN, "commands.reload.start");
    private static final MessageKey MSG_RELOAD_DONE_TYPES = MessageKey.of(ORIGIN, "commands.reload.types");
    private static final MessageKey MSG_RELOAD_DONE_RESTART = MessageKey.of(ORIGIN, "commands.reload.restart");

    private static final MessageKey MSG_REPORT_PLAYER_NOT_FOUND = MessageKey.of(ORIGIN, "commands.report.player_not_found");
    private static final MessageKey MSG_REPORT_NONE = MessageKey.of(ORIGIN, "commands.report.none");
    private static final MessageKey MSG_REPORT_TITLE = MessageKey.of(ORIGIN, "commands.report.title");
    private static final MessageKey MSG_REPORT_RESULT = MessageKey.of(ORIGIN, "commands.report.result");
    private static final MessageKey MSG_REPORT_REASON = MessageKey.of(ORIGIN, "commands.report.reason");
    private static final MessageKey MSG_REPORT_TRIGGER = MessageKey.of(ORIGIN, "commands.report.trigger");
    private static final MessageKey MSG_REPORT_MULTIBLOCK = MessageKey.of(ORIGIN, "commands.report.multiblock");
    private static final MessageKey MSG_REPORT_DEBUG_TITLE = MessageKey.of(ORIGIN, "commands.report.debug.title");
    private static final MessageKey MSG_REPORT_DEBUG_LINE = MessageKey.of(ORIGIN, "commands.report.debug.line");
    private static final MessageKey MSG_REPORT_TRACE_TITLE = MessageKey.of(ORIGIN, "commands.report.trace.title");
    private static final MessageKey MSG_REPORT_TRACE_LINE = MessageKey.of(ORIGIN, "commands.report.trace.line");

    private final MultiBlockEngine plugin;
    private final PlayerMessageService messageService;
    private final dev.darkblade.mbe.api.i18n.I18nService i18nService;

    public AdminCommand(MultiBlockEngine plugin, PlayerMessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.i18nService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.i18n.I18nService.class);
    }

    @Command("mbe admin status")
    @Permission("multiblockengine.status")
    public void handleStatus(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        MultiblockRuntimeService runtime = plugin.getManager();
        MetricsService metrics = runtime.getMetrics();
        
        sendMessage(sender, MSG_STATUS_TITLE, Map.of());
        sendMessage(sender, MSG_STATUS_LOADED_TYPES_VALUE, MessageUtils.params("value", runtime.getTypes().size()));
        sendMessage(sender, MSG_STATUS_TOTAL_CREATED_VALUE, MessageUtils.params("value", metrics.getCreatedInstances()));
        sendMessage(sender, MSG_STATUS_TOTAL_DESTROYED_VALUE, MessageUtils.params("value", metrics.getDestroyedInstances()));
        sendMessage(sender, MSG_STATUS_STRUCTURE_CHECKS_VALUE, MessageUtils.params("value", metrics.getStructureChecks()));
        sendMessage(sender, MSG_STATUS_AVG_TICK_VALUE, MessageUtils.params("value", String.format("%.1f s", metrics.getAverageTickTimeMs() / 1000.0D)));
    }

    @Command("mbe admin stats")
    @Permission("multiblockengine.status")
    public void handleStats(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        handleStatus(mbeSender);
    }

    @Command("mbe admin reload")
    @Permission("multiblockengine.admin.reload")
    public void handleReload(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        sendMessage(sender, MSG_RELOAD_START, Map.of());
        
        plugin.reloadConfig();

        if (i18nService != null) {
            i18nService.reload();
        }
        
        File multiblockDir = new File(plugin.getDataFolder(), "multiblocks");
        if (!multiblockDir.exists()) {
            multiblockDir.mkdirs();
        }
        
        MultiblockParser parser = plugin.getParser();
        List<MultiblockParser.LoadedType> loaded = parser.loadAllWithSources(multiblockDir);
        List<MultiblockType> newTypes = new ArrayList<>(loaded.size());
        java.util.Map<String, dev.darkblade.mbe.core.domain.MultiblockSource> sources = new java.util.HashMap<>();
        for (MultiblockParser.LoadedType lt : loaded) {
            if (lt == null || lt.type() == null) {
                continue;
            }
            newTypes.add(lt.type());
            sources.put(lt.type().id().toString(), lt.source());
        }

        plugin.getManager().reloadTypesWithSources(newTypes, sources);
        plugin.getManager().getMetrics().setEnabled(plugin.getConfig().getBoolean("metrics", true));

        plugin.getAddonLifecycleService().reloadAddons();
        plugin.getAddonLifecycleService().getServiceLifecycleOrchestrator().reloadAllServices();
        org.bukkit.Bukkit.getPluginManager().callEvent(new dev.darkblade.mbe.api.event.plugin.MbeReloadEvent());
        

        sendMessage(sender, MSG_RELOAD_DONE_TYPES, MessageUtils.params("count", newTypes.size()));
        sendMessage(sender, MSG_RELOAD_DONE_RESTART, Map.of());
    }

    @Command("mbe admin report [target]")
    @Permission("multiblockengine.report")
    public void handleReport(
            dev.darkblade.mbe.core.application.command.MBESender mbeSender,
            @Argument("target") Player targetArg,
            @Flag("console") boolean console
    ) {
        CommandSender sender = mbeSender.getSender();
        if (console && sender instanceof Player && !sender.hasPermission("multiblockengine.report.console")) {
            sendMessage(sender, CoreMessageKeys.COMMAND_NO_PERMISSION, Map.of());
            return;
        }

        Player target;
        if (targetArg != null) {
            target = targetArg;
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sendMessage(sender, MSG_REPORT_PLAYER_NOT_FOUND, MessageUtils.params("player", "<jugador>"));
            return;
        }

        if (plugin.getAssemblyCoordinator() == null) {
            sendMessage(sender, MSG_REPORT_NONE, Map.of());
            return;
        }

        AssemblyReport report = plugin.getAssemblyCoordinator().lastReport(target.getUniqueId()).orElse(null);
        if (report == null) {
            sendMessage(sender, MSG_REPORT_NONE, Map.of());
            return;
        }

        sendReport(sender, report);
        if (console) {
            sendReport(org.bukkit.Bukkit.getConsoleSender(), report);
        }
    }

    private void sendReport(CommandSender sender, AssemblyReport report) {
        sendMessage(sender, MSG_REPORT_TITLE, Map.of());
        sendMessage(sender, MSG_REPORT_RESULT, MessageUtils.params("result", report.success() ? "SUCCESS" : "FAILED"));
        if (report.reasonKey() != null && !report.reasonKey().isBlank()) {
            sendMessage(sender, MSG_REPORT_REASON, MessageUtils.params("reason", report.reasonKey()));
        }
        sendMessage(sender, MSG_REPORT_TRIGGER, MessageUtils.params("trigger", report.trigger()));
        sendMessage(sender, MSG_REPORT_MULTIBLOCK, MessageUtils.params("multiblock", report.multiblockId()));
        sendMessage(sender, MSG_REPORT_DEBUG_TITLE, Map.of());
        
        if (report.debugData().isEmpty()) {
            sendMessage(sender, MSG_REPORT_DEBUG_LINE, MessageUtils.params("key", "-", "value", "-"));
        } else {
            for (Map.Entry<String, Object> entry : report.debugData().entrySet()) {
                sendMessage(sender, MSG_REPORT_DEBUG_LINE, MessageUtils.params("key", entry.getKey(), "value", String.valueOf(entry.getValue())));
            }
        }
        sendMessage(sender, MSG_REPORT_TRACE_TITLE, Map.of());
        int i = 1;
        for (AssemblyStepTrace step : report.trace()) {
            if (step == null) {
                continue;
            }
            sendMessage(sender, MSG_REPORT_TRACE_LINE, MessageUtils.params("index", i++, "step", step.step(), "status", step.success() ? "OK" : "FAIL", "detail", step.detail()));
        }
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            if (i18nService != null) {
                i18nService.send(sender, key, params);
            }
        }
    }
}
