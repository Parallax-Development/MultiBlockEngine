package dev.darkblade.mbe.api.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;

public interface ToolAction {

    ActionId id();

    WrenchResult execute(WrenchContext context);
}
