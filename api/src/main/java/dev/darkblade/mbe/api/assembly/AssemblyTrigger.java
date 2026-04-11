package dev.darkblade.mbe.api.assembly;

import dev.darkblade.mbe.api.service.interaction.InteractionIntent;

public interface AssemblyTrigger {

    String id();

    boolean supports(InteractionIntent intent);

    boolean shouldTrigger(AssemblyContext context);
}
