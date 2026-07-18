package dev.darkblade.mbe.core.application.command.debug;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.application.command.MBECommandManager;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.internal.debug.DebugSessionService;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public class DebugCommand {
    private final MBECommandManager manager;
    private final MultiblockRuntimeService runtimeService;
    private final DebugSessionService debugSessionService;
    private final PlayerMessageService messageService;

    public DebugCommand(MBECommandManager manager, MultiblockRuntimeService runtimeService, DebugSessionService debugSessionService, PlayerMessageService messageService) {
        this.manager = manager;
        this.runtimeService = runtimeService;
        this.debugSessionService = debugSessionService;
        this.messageService = messageService;
    }

    public void register() {
        Command.Builder<dev.darkblade.mbe.core.application.command.MBESender> builder = manager.commandBuilder("mbe")
                .literal("debug")
                .permission("multiblockengine.debug");

        manager.command(builder.literal("type")
                .permission("multiblockengine.debug.session")
                .required(manager.componentBuilder(MultiblockType.class, "type"))
                .optional("target", org.incendo.cloud.bukkit.parser.PlayerParser.playerParser())
                .handler(context -> {
                    if (!context.sender().isPlayer()) return;
                    Player sender = context.sender().getPlayer();
                    MultiblockType type = context.get("type");
                    Player target = context.getOrDefault("target", sender);

                    runtimeService.getSource(type.id().toString()).ifPresent(src -> {
                        messageService.send(sender, new dev.darkblade.mbe.api.message.PlayerMessage(CoreMessageKeys.DEBUG_SOURCE, dev.darkblade.mbe.api.message.MessageChannel.SYSTEM, dev.darkblade.mbe.api.message.MessagePriority.NORMAL, dev.darkblade.mbe.api.i18n.MessageUtils.params("sourceType", src.type().name(), "path", src.path())));
                    });
                    messageService.send(sender, new dev.darkblade.mbe.api.message.PlayerMessage(CoreMessageKeys.DEBUG_SIGNATURE, dev.darkblade.mbe.api.message.MessageChannel.SYSTEM, dev.darkblade.mbe.api.message.MessagePriority.NORMAL, dev.darkblade.mbe.api.i18n.MessageUtils.params("signature", runtimeService.signatureOf(type))));

                    Block targetBlock = target.getTargetBlockExact(10);
                    if (targetBlock == null || targetBlock.getType().isAir()) {
                        messageService.send(sender, new dev.darkblade.mbe.api.message.PlayerMessage(MessageKey.of("mbe", "commands.debug.must_look_at_block"), dev.darkblade.mbe.api.message.MessageChannel.SYSTEM, dev.darkblade.mbe.api.message.MessagePriority.NORMAL, null));
                        return;
                    }

                    debugSessionService.startSession(target, type, targetBlock.getLocation());
                })
        );
    }
}
