package dev.darkblade.mbe.api.tool;

import java.util.Map;
import java.util.Optional;

public interface ToolMode {

    String id();

    Map<ActionTrigger, ActionId> bindings();

    default Optional<ActionId> resolve(ActionTrigger trigger) {
        return Optional.ofNullable(bindings().get(trigger));
    }
}
