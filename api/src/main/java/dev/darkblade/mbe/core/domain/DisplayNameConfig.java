package dev.darkblade.mbe.core.domain;

public record DisplayNameConfig(String text, boolean visible, String method) {
    public DisplayNameConfig {
        if (method == null) method = "hologram";
    }
}
