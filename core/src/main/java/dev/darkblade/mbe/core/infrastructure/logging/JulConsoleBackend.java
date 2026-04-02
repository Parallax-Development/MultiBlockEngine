package dev.darkblade.mbe.core.infrastructure.logging;

import dev.darkblade.mbe.api.logging.LogBackend;
import dev.darkblade.mbe.api.logging.LogEntry;
import dev.darkblade.mbe.api.logging.LogLevel;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JulConsoleBackend implements LogBackend {

    private final Logger logger;

    public JulConsoleBackend(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void publish(LogEntry entry) {
        String formatted = LogFormatter.format(entry, false);
        Level julLevel = toJulLevel(entry.level());
        if (entry.throwable() != null && entry.includeStacktrace()) {
            logger.log(julLevel, formatted, entry.throwable());
            return;
        }
        logger.log(julLevel, formatted);
    }

    private static Level toJulLevel(LogLevel level) {
        return switch (level) {
            case TRACE -> Level.FINEST;
            case DEBUG -> Level.FINE;
            case INFO -> Level.INFO;
            case WARN -> Level.WARNING;
            case ERROR, FATAL -> Level.SEVERE;
        };
    }
}
