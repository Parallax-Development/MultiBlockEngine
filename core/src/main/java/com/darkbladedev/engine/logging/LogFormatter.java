package com.darkbladedev.engine.logging;

import com.darkbladedev.engine.api.logging.LogEntry;
import com.darkbladedev.engine.api.logging.LogKv;

import java.util.List;

final class LogFormatter {

    private LogFormatter() {
    }

    static String format(LogEntry entry) {
        return format(entry, true);
    }

    static String format(LogEntry entry, boolean includeEngine) {
        StringBuilder sb = new StringBuilder();

        if (includeEngine) {
            sb.append('[').append(entry.engine()).append(']');
        }

        sb.append('[').append(entry.scope().label()).append(']')
            .append('[').append(entry.phase().name()).append(']')
            .append('[').append(entry.level().name()).append(']')
            .append(' ')
            .append(entry.message());

        List<LogKv> fields = entry.fields();
        boolean hasFields = fields != null && !fields.isEmpty();
        boolean hasTags = entry.tags() != null && !entry.tags().isEmpty();

        if (hasFields || hasTags) {
            if (hasFields) {
                for (int i = 0; i < fields.size(); i++) {
                    LogKv kv = fields.get(i);
                    if (kv == null) continue;
                    boolean last = isLastRenderableField(fields, i) && !hasTags;
                    sb.append('\n')
                        .append(last ? "  └─ " : "  ├─ ")
                        .append(kv.key())
                        .append(": ")
                        .append(String.valueOf(kv.value()));
                }
            }

            if (hasTags) {
                sb.append('\n')
                    .append("  └─ tags: ")
                    .append(String.join(", ", entry.tags()));
            }
        }

        if (entry.throwable() != null && !entry.includeStacktrace()) {
            Throwable root = Throwables.rootCause(entry.throwable());
            String rootMsg = root.getMessage();
            sb.append('\n')
                .append("Reason: ")
                .append(root.getClass().getSimpleName());
            if (rootMsg != null && !rootMsg.isBlank()) {
                sb.append(": ").append(rootMsg);
            }
        }

        return sb.toString();
    }

    private static boolean isLastRenderableField(List<LogKv> fields, int idx) {
        for (int i = idx + 1; i < fields.size(); i++) {
            if (fields.get(i) != null) {
                return false;
            }
        }
        return true;
    }
}
