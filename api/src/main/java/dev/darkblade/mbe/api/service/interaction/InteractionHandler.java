package dev.darkblade.mbe.api.service.interaction;

@FunctionalInterface
public interface InteractionHandler {

    boolean handle(InteractionIntent intent);
}
