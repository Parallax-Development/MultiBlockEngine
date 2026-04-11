package dev.darkblade.mbe.api.command;

@FunctionalInterface
public interface WrenchInteractable {
    WrenchResult onWrenchUse(WrenchContext context);
}

