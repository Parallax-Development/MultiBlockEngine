package dev.darkblade.mbe.api.service.interaction;

public interface InteractionPipelineService {

    boolean handle(InteractionIntent intent);

    void registerHandler(InteractionHandler handler);
}
