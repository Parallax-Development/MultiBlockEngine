package dev.darkblade.mbe.core.application.command.export;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.application.command.MBESender;
import dev.darkblade.mbe.core.internal.tooling.export.ExportSession;
import dev.darkblade.mbe.core.internal.tooling.export.SelectionService;
import dev.darkblade.mbe.core.internal.tooling.export.StructureExporter;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

import java.nio.file.Path;
import java.util.Map;

public class ExportCommand {
    private final SelectionService exportSelections;
    private final StructureExporter structureExporter;
    private final PlayerMessageService messageService;
    private final Path exportDirectory;

    public ExportCommand(
            SelectionService exportSelections,
            StructureExporter structureExporter,
            PlayerMessageService messageService,
            Path exportDirectory
    ) {
        this.exportSelections = exportSelections;
        this.structureExporter = structureExporter;
        this.messageService = messageService;
        this.exportDirectory = exportDirectory;
    }

    @Command("mbe structure export start")
    @Permission("multiblockengine.export")
    public void start(MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) return;
        exportSelections.start(player);
        sendMessage(player, CoreMessageKeys.EXPORT_STARTED, Map.of());
    }

    @Command("mbe structure export cancel")
    @Permission("multiblockengine.export")
    public void cancel(MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) return;
        boolean cancelled = exportSelections.cancel(player);
        sendMessage(player, cancelled ? CoreMessageKeys.EXPORT_CANCELLED : CoreMessageKeys.EXPORT_NO_ACTIVE, Map.of());
    }

    @Command("mbe structure export pos1")
    @Permission("multiblockengine.export")
    public void pos1(MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) return;
        ExportSession s = ensureSession(player);
        if (s == null) return;
        Block b = player.getTargetBlockExact(10);
        if (b == null) {
            sendMessage(player, CoreMessageKeys.EXPORT_MUST_LOOK, Map.of());
            return;
        }
        s.setPos1(b.getLocation());
        sendMessage(player, CoreMessageKeys.EXPORT_POS1_SET, MessageUtils.params("x", b.getX(), "y", b.getY(), "z", b.getZ()));
    }

    @Command("mbe structure export pos2")
    @Permission("multiblockengine.export")
    public void pos2(MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) return;
        ExportSession s = ensureSession(player);
        if (s == null) return;
        Block b = player.getTargetBlockExact(10);
        if (b == null) {
            sendMessage(player, CoreMessageKeys.EXPORT_MUST_LOOK, Map.of());
            return;
        }
        s.setPos2(b.getLocation());
        sendMessage(player, CoreMessageKeys.EXPORT_POS2_SET, MessageUtils.params("x", b.getX(), "y", b.getY(), "z", b.getZ()));
    }

    @Command("mbe structure export mark <role>")
    @Permission("multiblockengine.export")
    public void mark(MBESender mbeSender, @Argument("role") String role) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) return;
        ExportSession s = ensureSession(player);
        if (s == null) return;
        s.setPendingRole(role);
        sendMessage(player, CoreMessageKeys.EXPORT_MARK_PROMPT, MessageUtils.params("role", role));
    }

    @Command("mbe structure export save <id>")
    @Permission("multiblockengine.export")
    public void save(MBESender mbeSender, @Argument("id") String id) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) return;
        ExportSession s = ensureSession(player);
        if (s == null) return;
        try {
            var res = structureExporter.exportToFile(id, s, exportDirectory);
            sendMessage(player, CoreMessageKeys.EXPORT_SAVE_OK, MessageUtils.params("id", res.id(), "blocks", res.blocks()));
            if (!res.warnings().isEmpty()) {
                sendMessage(player, CoreMessageKeys.EXPORT_SAVE_WARNINGS, MessageUtils.params("count", res.warnings().size()));
            }
        } catch (StructureExporter.ExportException e) {
            sendMessage(player, CoreMessageKeys.EXPORT_SAVE_ERROR, MessageUtils.params("error", e.getMessage()));
        }
    }

    private ExportSession ensureSession(Player player) {
        ExportSession s = exportSelections.session(player);
        if (s == null) {
            sendMessage(player, CoreMessageKeys.EXPORT_NO_ACTIVE, Map.of());
        }
        return s;
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            org.bukkit.Bukkit.getLogger().info(key.fullKey() + " " + params.toString());
        }
    }
}
