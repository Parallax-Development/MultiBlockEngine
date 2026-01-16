package com.darkbladedev.engine.api.addon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionTest {

    @Test
    void parse_validSemver() {
        Version v = Version.parse("1.2.3");
        assertEquals(1, v.major());
        assertEquals(2, v.minor());
        assertEquals(3, v.patch());
        assertTrue(v.preRelease().isEmpty());
        assertNull(v.build());
        assertEquals("1.2.3", v.raw());
    }

    @Test
    void compare_preReleaseOrdering() {
        Version stable = Version.parse("1.0.0");
        Version alpha = Version.parse("1.0.0-alpha");
        Version alpha2 = Version.parse("1.0.0-alpha.2");
        Version alpha10 = Version.parse("1.0.0-alpha.10");

        assertTrue(stable.compareTo(alpha) > 0);
        assertTrue(alpha.compareTo(alpha2) < 0);
        assertTrue(alpha2.compareTo(alpha10) < 0);
    }

    @Test
    void isAtLeast_works() {
        Version v = Version.parse("1.2.3");
        assertTrue(v.isAtLeast(Version.parse("1.2.3")));
        assertTrue(v.isAtLeast(Version.parse("1.2.0")));
        assertFalse(v.isAtLeast(Version.parse("2.0.0")));
    }

    @Test
    void parse_rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Version.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("01.2.3"));
    }
}
