package dev.darkblade.mbe.core.infrastructure.logging;

import dev.darkblade.mbe.api.logging.LogEntry;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LogFormatterTest {

    @Test
    void format_includesEngineByDefault() {
        LogEntry entry = new LogEntry(
            Instant.EPOCH,
            "MultiBlockEngine",
            new LogScope.Core(),
            LogPhase.BOOT,
            LogLevel.INFO,
            "hello",
            List.of(),
            Set.of(),
            null,
            false
        );

        String formatted = LogFormatter.format(entry);
        assertTrue(formatted.startsWith("[MultiBlockEngine][CORE][BOOT][INFO] hello"));
    }

    @Test
    void format_withoutEngine_excludesEnginePrefix() {
        LogEntry entry = new LogEntry(
            Instant.EPOCH,
            "MultiBlockEngine",
            new LogScope.Core(),
            LogPhase.BOOT,
            LogLevel.INFO,
            "hello",
            List.of(),
            Set.of(),
            null,
            false
        );

        String formatted = LogFormatter.format(entry, false);
        assertTrue(formatted.startsWith("[CORE][BOOT][INFO] hello"));
        assertFalse(formatted.contains("MultiBlockEngine"));
    }
}

