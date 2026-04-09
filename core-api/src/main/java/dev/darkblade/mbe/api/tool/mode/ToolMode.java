package dev.darkblade.mbe.api.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;

public interface ToolMode {

    String getId();

    String getDisplayName();

    boolean supports(WrenchContext context);

    WrenchResult handle(WrenchContext context);
}
