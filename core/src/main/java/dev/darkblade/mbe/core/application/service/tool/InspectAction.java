package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
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
    private final I18nService i18n;

    public InspectAction(MultiblockRuntimeService runtimeService, I18nService i18n) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
        this.i18n = i18n;
    }

    @Override
    public ActionId id() {
        return WrenchActions.INSPECT;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        Optional<MultiblockInstance> instanceOpt = runtimeService.getInstanceAt(context.clickedBlock().getLocation());
        if (instanceOpt.isEmpty()) {
            if (i18n != null && context.player() != null) {
                i18n.send(context.player(), MSG_NOT_FOUND);
            }
            return WrenchResult.fail(MSG_NOT_FOUND.path());
        }
        MultiblockInstance instance = instanceOpt.get();
        if (i18n != null && context.player() != null) {
            i18n.send(context.player(), MSG_TITLE);
            i18n.send(context.player(), MSG_TYPE, Map.of("type", safe(instance.type().id())));
            i18n.send(context.player(), MSG_STATE, Map.of("state", instance.state() == null ? "" : instance.state().name()));
            i18n.send(context.player(), MSG_FACING, Map.of("facing", instance.facing() == null ? "" : instance.facing().name()));
            i18n.send(context.player(), MSG_ANCHOR, Map.of("anchor", formatLocation(instance.anchorLocation())));
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
