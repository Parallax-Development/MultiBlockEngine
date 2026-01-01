package com.darkbladedev.engine.api.logging;

public sealed interface LogScope permits LogScope.Core, LogScope.Addon {

    String label();

    record Core() implements LogScope {
        @Override
        public String label() {
            return "CORE";
        }
    }

    record Addon(String addonId, String addonVersion) implements LogScope {
        @Override
        public String label() {
            return "Addon:" + addonId + "@" + addonVersion;
        }
    }
}

