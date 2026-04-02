package dev.darkblade.mbe.api.assembly;

import java.util.Locale;

public enum AssemblyTriggerType {
    WRENCH_USE,
    SNEAK_RIGHT_CLICK,
    ON_FINAL_BLOCK_PLACED,
    MANUAL_ONLY;

    public String id() {
        return "mbe:" + name().toLowerCase(Locale.ROOT);
    }
}
