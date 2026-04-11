package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DisassembleAction implements ToolAction {

    private static final MessageKey MSG_DISASSEMBLED = MessageKey.of("mbe", "core.wrench.disassembled");
    private static final MessageKey MSG_NOT_FOUND = MessageKey.of("mbe", "core.wrench.not_found");

    private final MultiblockRuntimeService runtimeService;
    private final I18nService i18n;

    public DisassembleAction(MultiblockRuntimeService runtimeService, I18nService i18n) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
        this.i18n = i18n;
    }

    @Override
    public ActionId id() {
        return WrenchActions.DISASSEMBLE;
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
        runtimeService.destroyInstance(instance);
        if (i18n != null && context.player() != null) {
            i18n.send(context.player(), MSG_DISASSEMBLED, Map.of("type", instance.type().id()));
        }
        return WrenchResult.success(MSG_DISASSEMBLED.path(), Map.of("type", instance.type().id()));
    }
}
