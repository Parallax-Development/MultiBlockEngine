package com.darkbladedev.engine.api.persistence;

import java.util.List;

public record StorageRecoveryReport(
    boolean hadWork,
    List<StorageRecoveryAction> actions
) {
    public StorageRecoveryReport {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static StorageRecoveryReport empty() {
        return new StorageRecoveryReport(false, List.of());
    }

    public record StorageRecoveryAction(
        String type,
        String namespace,
        String domain,
        String store,
        String detail
    ) {
    }
}
