package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionSource;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;

import java.util.Map;
import java.util.Objects;

public final class AssembleAction implements ToolAction {

    private static final MessageKey MSG_ASSEMBLED = CoreMessageKeys.WRENCH_ASSEMBLED;

    private final AssemblyCoordinator assemblyCoordinator;
    private final PlayerMessageService messageService;

    public AssembleAction(AssemblyCoordinator assemblyCoordinator, PlayerMessageService messageService) {
        this.assemblyCoordinator = Objects.requireNonNull(assemblyCoordinator, "assemblyCoordinator");
        this.messageService = messageService;
    }

    @Override
    public ActionId id() {
        return WrenchActions.ASSEMBLE;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        if (context.clickedBlock() == null) {
            return WrenchResult.pass();
        }
        AssemblyContext assemblyContext = new AssemblyContext(
                context.player(),
                context.clickedBlock(),
                new InteractionIntent(
                        context.player(),
                        InteractionType.WRENCH_USE,
                        context.clickedBlock(),
                        context.item(),
                        InteractionSource.WRENCH
                )
        );
        AssemblyReport report = assemblyCoordinator.attemptAssembly(assemblyContext);
        if (report == null || !report.success()) {
            return WrenchResult.noop();
        }
        if (messageService != null && context.player() != null) {
            messageService.send(context.player(), new PlayerMessage(
                    MSG_ASSEMBLED,
                    MessageChannel.CHAT,
                    MessagePriority.NORMAL,
                    Map.of("type", safeType(report.multiblockId()))
            ));
        }
        return WrenchResult.success(MSG_ASSEMBLED.path(), Map.of("type", safeType(report.multiblockId())));
    }

    private String safeType(String id) {
        if (id == null || id.isBlank()) {
            return "unknown";
        }
        return id;
    }
}
