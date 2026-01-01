package com.darkbladedev.engine.api.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version implements Comparable<Version> {

    private static final Pattern SEMVER = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
            "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?" +
            "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$"
    );

    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> preRelease;
    private final String build;
    private final String raw;

    private Version(int major, int minor, int patch, List<String> preRelease, String build, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = List.copyOf(preRelease);
        this.build = build;
        this.raw = raw;
    }

    public static Version parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("version");
        }

        String v = value.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("version");
        }

        Matcher m = SEMVER.matcher(v);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid SemVer: " + value);
        }

        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = Integer.parseInt(m.group(3));

        List<String> pre = new ArrayList<>();
        String preStr = m.group(4);
        if (preStr != null) {
            for (String part : preStr.split("\\.")) {
                if (part.isEmpty()) {
                    throw new IllegalArgumentException("Invalid SemVer pre-release: " + value);
                }
                pre.add(part);
            }
        }

        String build = m.group(5);
        return new Version(major, minor, patch, pre, build, v);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public List<String> preRelease() {
        return preRelease;
    }

    public String build() {
        return build;
    }

    public String raw() {
        return raw;
    }

    public boolean isAtLeast(Version min) {
        return compareTo(min) >= 0;
    }

    @Override
    public int compareTo(Version other) {
        Objects.requireNonNull(other, "other");

        int c;
        c = Integer.compare(this.major, other.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, other.minor);
        if (c != 0) return c;
        c = Integer.compare(this.patch, other.patch);
        if (c != 0) return c;

        boolean thisPre = !this.preRelease.isEmpty();
        boolean otherPre = !other.preRelease.isEmpty();

        if (!thisPre && !otherPre) {
            return 0;
        }
        if (!thisPre) {
            return 1;
        }
        if (!otherPre) {
            return -1;
        }

        int len = Math.min(this.preRelease.size(), other.preRelease.size());
        for (int i = 0; i < len; i++) {
            String a = this.preRelease.get(i);
            String b = other.preRelease.get(i);

            boolean aNum = isNumeric(a);
            boolean bNum = isNumeric(b);

            if (aNum && bNum) {
                long ai = Long.parseLong(a);
                long bi = Long.parseLong(b);
                int pc = Long.compare(ai, bi);
                if (pc != 0) return pc;
                continue;
            }

            if (aNum) return -1;
            if (bNum) return 1;

            int sc = a.toLowerCase(Locale.ROOT).compareTo(b.toLowerCase(Locale.ROOT));
            if (sc != 0) return sc;
        }

        return Integer.compare(this.preRelease.size(), other.preRelease.size());
    }

    @Override
    public String toString() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version that)) return false;
        return this.compareTo(that) == 0 && Objects.equals(this.build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease, build);
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return true;
    }
}

