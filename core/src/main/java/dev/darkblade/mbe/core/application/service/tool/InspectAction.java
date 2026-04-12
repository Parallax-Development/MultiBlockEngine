package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InspectAction implements ToolAction {

    private static final MessageKey MSG_NOT_FOUND = MessageKey.of("mbe", "core.wrench.not_found");
    private static final MessageKey MSG_TITLE = MessageKey.of("mbe", "core.wrench.inspect.title");
    private static final MessageKey MSG_TYPE = MessageKey.of("mbe", "core.wrench.inspect.type");
    private static final MessageKey MSG_STATE = MessageKey.of("mbe", "core.wrench.inspect.state");
    private static final MessageKey MSG_FACING = MessageKey.of("mbe", "core.wrench.inspect.facing");
    private static final MessageKey MSG_ANCHOR = MessageKey.of("mbe", "core.wrench.inspect.anchor");

    private final MultiblockRuntimeService runtimeService;
    private final PlayerMessageService messageService;

    public InspectAction(MultiblockRuntimeService runtimeService, PlayerMessageService messageService) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
        this.messageService = messageService;
    }

    @Override
    public ActionId id() {
        return WrenchActions.INSPECT;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        Optional<MultiblockInstance> instanceOpt = runtimeService.getInstanceAt(context.clickedBlock().getLocation());
        if (instanceOpt.isEmpty()) {
            if (messageService != null && context.player() != null) {
                messageService.send(context.player(), new PlayerMessage(MSG_NOT_FOUND, MessageChannel.CHAT, MessagePriority.HIGH, Map.of()));
            }
            return WrenchResult.fail(MSG_NOT_FOUND.path());
        }
        MultiblockInstance instance = instanceOpt.get();
        if (messageService != null && context.player() != null) {
            messageService.send(context.player(), new PlayerMessage(MSG_TITLE, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of()));
            messageService.send(context.player(), new PlayerMessage(MSG_TYPE, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of("type", safe(instance.type().id()))));
            messageService.send(context.player(), new PlayerMessage(MSG_STATE, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of("state", instance.state() == null ? "" : instance.state().name())));
            messageService.send(context.player(), new PlayerMessage(MSG_FACING, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of("facing", instance.facing() == null ? "" : instance.facing().name())));
            messageService.send(context.player(), new PlayerMessage(MSG_ANCHOR, MessageChannel.CHAT, MessagePriority.NORMAL, Map.of("anchor", formatLocation(instance.anchorLocation()))));
        }
        return WrenchResult.success(MSG_TITLE.path());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
