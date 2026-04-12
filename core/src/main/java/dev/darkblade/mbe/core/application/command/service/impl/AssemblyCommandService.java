package dev.darkblade.mbe.core.application.command.service.impl;

import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.command.MbeCommandService;
import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AssemblyCommandService implements MbeCommandService {
    private static final MessageKey MSG_UNKNOWN_SUBCOMMAND = MessageKey.of("mbe", "commands.error.unknown_subcommand");

    private final AssemblyCoordinator assembly;
    private final MultiblockRuntimeService manager;
    private final I18nService i18n;
    private final PlayerMessageService messageService;
    private final CoreLogger log;
    private final MultiBlockEngine plugin;

    public AssemblyCommandService(MultiBlockEngine plugin) {
        this.plugin = plugin;
        this.assembly = plugin.getAssemblyCoordinator();
        this.manager = plugin.getManager();
        this.i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        this.messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
        this.log = plugin.getLoggingService().core();
    }

    @Override
    public String id() {
        return "assembly";
    }

    @Override
    public List<String> aliases() {
        return List.of("assemble", "disassemble");
    }

    @Override
    public String description() {
        return "Assembly and disassembly operations";
    }

    @Override
    public List<String> infoUsage() {
        return List.of("/mbe services call assembly info");
    }

    @Override
    public List<String> executeUsage() {
        return List.of(
                "/mbe services call assembly execute assemble",
                "/mbe services call assembly execute disassemble"
        );
    }

    @Override
    public void info(CommandSender sender, List<String> args) {
        for (String line : executeUsage()) {
            send(sender, MessageKey.of("mbe", line));
        }
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player player)) {
            send(sender, CoreMessageKeys.COMMAND_PLAYER_ONLY);
            return;
        }
        String action = args == null || args.isEmpty() || args.get(0) == null ? "assemble" : args.get(0).trim().toLowerCase(Locale.ROOT);
        if ("assemble".equals(action) || "form".equals(action)) {
            executeAssemble(player);
            return;
        }
        if ("disassemble".equals(action) || "break".equals(action)) {
            executeDisassemble(player);
            return;
        }
        send(sender, MSG_UNKNOWN_SUBCOMMAND);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
        if (!"execute".equalsIgnoreCase(mode)) {
            return List.of();
        }
        if (args == null || args.size() <= 1) {
            String input = args == null || args.isEmpty() || args.get(0) == null ? "" : args.get(0).toLowerCase(Locale.ROOT);
            return List.of("assemble", "disassemble").stream().filter(v -> v.startsWith(input)).toList();
        }
        return List.of();
    }

    public void executeAssemble(Player player) {
        if (assembly == null) {
            log.error("Assembly command failed: coordinator unavailable", LogKv.kv("player", player.getName()));
            send(player, CoreMessageKeys.ASSEMBLY_COORDINATOR_UNAVAILABLE);
            return;
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            send(player, CoreMessageKeys.ASSEMBLE_MUST_LOOK);
            return;
        }
        AssemblyContext ctx = new AssemblyContext(
                player,
                target,
                new dev.darkblade.mbe.api.service.interaction.InteractionIntent(
                        player,
                        dev.darkblade.mbe.api.service.interaction.InteractionType.PROGRAMMATIC,
                        target,
                        null,
                        dev.darkblade.mbe.api.service.interaction.InteractionSource.PROGRAMMATIC
                )
        );
        AssemblyReport report = assembly.attemptAssembly(ctx);
        if (report == null) {
            log.error("Assembly command failed: null report", LogKv.kv("player", player.getName()));
            send(player, CoreMessageKeys.ASSEMBLE_TRY_FAILED);
            return;
        }
        log.info(
                "Assembly command attempted",
                LogKv.kv("player", player.getName()),
                LogKv.kv("success", report.success()),
                LogKv.kv("trigger", report.trigger()),
                LogKv.kv("multiblock", report.multiblockId()),
                LogKv.kv("reason", report.reasonKey())
        );
        if (report.success()) {
            send(player, CoreMessageKeys.ASSEMBLE_OK, java.util.Map.of("id", report.multiblockId()));
            return;
        }
        if ("limit_reached".equalsIgnoreCase(report.reasonKey())) {
            if (report.debugData().containsKey("current") && report.debugData().containsKey("max")) {
                send(player, CoreMessageKeys.LIMIT_REACHED);
            } else {
                sendReasonFeedback(player, report.reasonKey());
            }
            return;
        }
        if (sendReasonFeedback(player, report.reasonKey())) {
            return;
        }
        String reason = report.reasonKey() == null || report.reasonKey().isBlank() ? "unknown" : report.reasonKey();
        send(player, CoreMessageKeys.ASSEMBLE_FAILED, Map.of("reason", reason));
    }

    public void executeDisassemble(Player player) {
        if (manager == null) {
            log.error("Disassemble command failed: manager unavailable", LogKv.kv("player", player.getName()));
            send(player, CoreMessageKeys.DISASSEMBLE_NONE_HERE);
            return;
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            send(player, CoreMessageKeys.DISASSEMBLE_MUST_LOOK);
            return;
        }
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(target.getLocation());
        if (instanceOpt.isEmpty()) {
            send(player, CoreMessageKeys.DISASSEMBLE_NONE_HERE);
            return;
        }
        MultiblockInstance instance = instanceOpt.get();
        MultiblockBreakEvent event = new MultiblockBreakEvent(instance, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            send(player, CoreMessageKeys.ACTION_CANCELLED);
            return;
        }
        for (dev.darkblade.mbe.core.domain.action.Action action : instance.type().onBreakActions()) {
            if (action == null) {
                continue;
            }
            try {
                action.execute(instance, player);
            } catch (Throwable t) {
                log.error(
                        "Disassemble break action failed",
                        t,
                        LogKv.kv("player", player.getName()),
                        LogKv.kv("multiblock", instance.type().id()),
                        LogKv.kv("action", action.getClass().getSimpleName())
                );
            }
        }
        MultiblockLimitService limitService = resolveLimitService();
        if (limitService != null) {
            limitService.unregisterAssembly(player.getUniqueId(), instance.type().id());
        }
        manager.destroyInstance(instance);
        log.info("Disassemble command succeeded", LogKv.kv("player", player.getName()), LogKv.kv("multiblock", instance.type().id()));
        send(player, CoreMessageKeys.DISASSEMBLED);
    }

    private void send(CommandSender sender, MessageKey key) {
        if (sender == null || key == null) {
            return;
        }
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of()));
            return;
        }
        if (i18n != null) {
            i18n.send(sender, key);
        }
    }

    private void send(CommandSender sender, MessageKey key, java.util.Map<String, ?> params) {
        if (sender == null || key == null) {
            return;
        }
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, castParams(params)));
            return;
        }
        if (i18n != null) {
            i18n.send(sender, key, params);
        }
    }

    private MultiblockLimitService resolveLimitService() {
        if (plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(MultiblockLimitService.class);
    }

    private boolean sendReasonFeedback(Player player, String reasonKey) {
        if (i18n == null || player == null || reasonKey == null || reasonKey.isBlank()) {
            return false;
        }
        MessageKey key = MessageKey.of("mbe", "commands.error." + reasonKey);
        String translated = i18n.tr(player, key);
        if (translated == null || translated.isBlank() || translated.equals(key.path())) {
            return false;
        }
        send(player, key);
        return true;
    }

    private Map<String, Object> castParams(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new java.util.HashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }
}
