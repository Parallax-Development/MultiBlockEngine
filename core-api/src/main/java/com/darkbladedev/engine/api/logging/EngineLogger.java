package com.darkbladedev.engine.api.logging;

public interface EngineLogger {

    void log(LogLevel level, String message, Throwable throwable, LogKv... fields);

    default void trace(String message, LogKv... fields) {
        log(LogLevel.TRACE, message, null, fields);
    }

    default void debug(String message, LogKv... fields) {
        log(LogLevel.DEBUG, message, null, fields);
    }

    default void info(String message, LogKv... fields) {
        log(LogLevel.INFO, message, null, fields);
    }

    default void warn(String message, LogKv... fields) {
        log(LogLevel.WARN, message, null, fields);
    }

    default void error(String message, LogKv... fields) {
        log(LogLevel.ERROR, message, null, fields);
    }

    default void error(String message, Throwable throwable, LogKv... fields) {
        log(LogLevel.ERROR, message, throwable, fields);
    }

    default void fatal(String message, LogKv... fields) {
        log(LogLevel.FATAL, message, null, fields);
    }

    default void fatal(String message, Throwable throwable, LogKv... fields) {
        log(LogLevel.FATAL, message, throwable, fields);
    }
}

