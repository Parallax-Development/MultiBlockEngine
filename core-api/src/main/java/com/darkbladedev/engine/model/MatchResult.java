package com.darkbladedev.engine.model;

public record MatchResult(boolean success, String reason) {

    public static MatchResult ok() {
        return new MatchResult(true, "");
    }

    public static MatchResult fail(String reason) {
        return new MatchResult(false, reason == null ? "" : reason);
    }
}
