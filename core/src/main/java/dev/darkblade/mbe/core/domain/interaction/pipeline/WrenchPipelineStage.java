package dev.darkblade.mbe.core.domain.interaction.pipeline;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;

public interface WrenchPipelineStage {
    WrenchResult process(WrenchContext context);
}
