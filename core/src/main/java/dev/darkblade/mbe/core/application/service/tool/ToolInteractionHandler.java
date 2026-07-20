package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.service.interaction.InteractionHandler;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

public final class ToolInteractionHandler implements InteractionHandler {

    private final ToolDispatcher toolDispatcher;

    public ToolInteractionHandler(ToolDispatcher toolDispatcher) {
        this.toolDispatcher = toolDispatcher;
    }

    @Override
    public boolean handle(InteractionIntent intent) {
        if (intent == null || intent.player() == null || intent.itemInHand() == null) {
            return false;
        }

        Action action = toBukkitAction(intent.type());
        if (action == null) {
            return false;
        }

        WrenchContext ctx = new WrenchContext(
                intent.player(),
                intent.targetBlock(),
                action,
                intent.itemInHand(),
                EquipmentSlot.HAND
        );

        if (!toolDispatcher.isToolItem(ctx)) {
            return false;
        }

        ActionTrigger trigger = resolveTrigger(intent);
        WrenchResult result = toolDispatcher.dispatch(ctx, trigger);
        return result != null && !result.isPass();
    }

    private Action toBukkitAction(InteractionType type) {
        if (type == InteractionType.LEFT_CLICK_BLOCK) {
            return Action.LEFT_CLICK_BLOCK;
        }
        if (type == InteractionType.RIGHT_CLICK_BLOCK
                || type == InteractionType.SHIFT_RIGHT_CLICK || type == InteractionType.WRENCH_USE) {
            return Action.RIGHT_CLICK_BLOCK;
        }
        return null;
    }

    private ActionTrigger resolveTrigger(InteractionIntent intent) {
        boolean isRightClick = intent.type() == InteractionType.RIGHT_CLICK_BLOCK
                || intent.type() == InteractionType.SHIFT_RIGHT_CLICK
                || intent.type() == InteractionType.WRENCH_USE;

        boolean isLeftClick = intent.type() == InteractionType.LEFT_CLICK_BLOCK;

        boolean isSneaking = intent.player().isSneaking();

        if (intent.type() == InteractionType.SHIFT_RIGHT_CLICK) {
            return ActionTrigger.SHIFT_RIGHT_CLICK;
        }

        if (isRightClick && isSneaking) {
            return ActionTrigger.SHIFT_RIGHT_CLICK;
        }
        if (isLeftClick && isSneaking) {
            return ActionTrigger.SHIFT_LEFT_CLICK;
        }
        if (isRightClick) {
            return ActionTrigger.RIGHT_CLICK;
        }
        return ActionTrigger.LEFT_CLICK;
    }
}
