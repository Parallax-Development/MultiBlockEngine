package dev.darkblade.mbe.api.compat;

import java.util.Locale;
import java.util.Objects;

public record ServerVersion(int major, int minor, int patch) implements Comparable<ServerVersion> {
    public static final ServerVersion UNKNOWN = new ServerVersion(0, 0, 0);

    public ServerVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must be >= 0");
        }
    }

    public static ServerVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        int dash = value.indexOf('-');
        if (dash >= 0) {
            value = value.substring(0, dash);
        }
        String[] parts = value.split("\\.");
        if (parts.length < 2) {
            return UNKNOWN;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
            return new ServerVersion(major, minor, patch);
        } catch (NumberFormatException ignored) {
            return UNKNOWN;
        }
    }

    public boolean isUnknown() {
        return equals(UNKNOWN);
    }

    public boolean isAtLeast(ServerVersion other) {
        Objects.requireNonNull(other, "other");
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(ServerVersion other) {
        if (other == null) {
            return 1;
        }
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
