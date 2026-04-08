package dev.darkblade.mbe.api.command;

import java.util.Map;

public record WrenchResult(
        WrenchResultType type,
        String messageKey,
        Map<String, Object> context
) {

    public WrenchResult {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (context == null) {
            context = Map.of();
        } else {
            context = Map.copyOf(context);
        }
    }

    public static WrenchResult success(String key) {
        return new WrenchResult(WrenchResultType.SUCCESS, key, Map.of());
    }

    public static WrenchResult success(String key, Map<String, Object> context) {
        return new WrenchResult(WrenchResultType.SUCCESS, key, context);
    }

    public static WrenchResult fail(String key) {
        return new WrenchResult(WrenchResultType.FAIL, key, Map.of());
    }

    public static WrenchResult fail(String key, Map<String, Object> context) {
        return new WrenchResult(WrenchResultType.FAIL, key, context);
    }

    public static WrenchResult pass() {
        return new WrenchResult(WrenchResultType.PASS, null, Map.of());
    }

    public boolean isSuccess() {
        return type == WrenchResultType.SUCCESS;
    }

    public boolean isFail() {
        return type == WrenchResultType.FAIL;
    }

    public boolean isPass() {
        return type == WrenchResultType.PASS;
    }
}
