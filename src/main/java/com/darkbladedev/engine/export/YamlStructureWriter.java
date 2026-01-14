package com.darkbladedev.engine.export;

import com.darkbladedev.engine.api.export.ExportBlockPos;
import com.darkbladedev.engine.api.export.ExportBlockSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class YamlStructureWriter {

    private YamlStructureWriter() {
    }

    static String write(
            String id,
            ExportBlockPos controller,
            String controllerMatch,
            Map<BlockKey, ExportBlockSnapshot> snapshots,
            DefaultExportContext ctx
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(controllerMatch, "controllerMatch");
        Objects.requireNonNull(snapshots, "snapshots");
        Objects.requireNonNull(ctx, "ctx");

        StringBuilder sb = new StringBuilder();

        sb.append("id: ").append(id).append('\n');
        sb.append("version: \"1.0\"\n");
        sb.append("controller: ").append(escapeScalar(controllerMatch)).append('\n');
        sb.append("pattern:\n");

        List<OffsetEntry> entries = new ArrayList<>();
        for (ExportBlockSnapshot snap : snapshots.values()) {
            if (snap == null) {
                continue;
            }
            if (samePos(controller, snap.pos())) {
                continue;
            }
            int dx = snap.pos().x() - controller.x();
            int dy = snap.pos().y() - controller.y();
            int dz = snap.pos().z() - controller.z();
            String match = snap.blockData() != null && snap.blockData().contains("[") ? snap.blockData() : snap.material().name();
            entries.add(new OffsetEntry(dx, dy, dz, match));
        }

        entries.sort(Comparator
                .comparingInt(OffsetEntry::dy)
                .thenComparingInt(OffsetEntry::dx)
                .thenComparingInt(OffsetEntry::dz)
        );

        for (OffsetEntry e : entries) {
            sb.append("  - offset: [").append(e.dx).append(", ").append(e.dy).append(", ").append(e.dz).append("]\n");
            sb.append("    match: ").append(escapeScalar(e.match)).append('\n');
        }

        String roles = writeRoles(controller, ctx);
        if (!roles.isEmpty()) {
            sb.append(roles);
        }

        String props = writeProperties(controller, ctx);
        if (!props.isEmpty()) {
            sb.append(props);
        }

        List<String> warnings = ctx.warnings();
        if (!warnings.isEmpty()) {
            sb.append("warnings:\n");
            for (String w : warnings) {
                sb.append("  - ").append(escapeScalar(w)).append('\n');
            }
        }

        return sb.toString();
    }

    private static String writeRoles(ExportBlockPos controller, DefaultExportContext ctx) {
        Map<String, List<Offset>> byRole = new LinkedHashMap<>();

        for (Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, String> e : ctx.roles().entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            String role = e.getValue().trim().toLowerCase(Locale.ROOT);
            int dx = e.getKey().x() - controller.x();
            int dy = e.getKey().y() - controller.y();
            int dz = e.getKey().z() - controller.z();
            byRole.computeIfAbsent(role, k -> new ArrayList<>()).add(new Offset(dx, dy, dz));
        }

        if (byRole.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("roles:\n");
        List<String> roles = new ArrayList<>(byRole.keySet());
        roles.sort(String::compareToIgnoreCase);
        for (String role : roles) {
            List<Offset> list = byRole.get(role);
            if (list == null || list.isEmpty()) {
                continue;
            }
            list.sort(Comparator.comparingInt((Offset o) -> o.dy).thenComparingInt(o -> o.dx).thenComparingInt(o -> o.dz));
            sb.append("  ").append(role).append(":\n");
            for (Offset o : list) {
                sb.append("    - [").append(o.dx).append(", ").append(o.dy).append(", ").append(o.dz).append("]\n");
            }
        }
        return sb.toString();
    }

    private static String writeProperties(ExportBlockPos controller, DefaultExportContext ctx) {
        Map<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> props = ctx.properties();
        if (props.isEmpty()) {
            return "";
        }

        List<Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>>> entries = new ArrayList<>(props.entrySet());
        entries.sort(Comparator
                .comparingInt((Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> e) -> e.getKey().y())
                .thenComparingInt(e -> e.getKey().x())
                .thenComparingInt(e -> e.getKey().z())
        );

        StringBuilder sb = new StringBuilder();
        sb.append("properties:\n");
        for (Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> e : entries) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            int dx = e.getKey().x() - controller.x();
            int dy = e.getKey().y() - controller.y();
            int dz = e.getKey().z() - controller.z();

            sb.append("  - offset: [").append(dx).append(", ").append(dy).append(", ").append(dz).append("]\n");

            List<String> keys = new ArrayList<>(e.getValue().keySet());
            keys.removeIf(k -> k == null || k.isBlank());
            keys.sort(String::compareToIgnoreCase);
            for (String k : keys) {
                Object v = e.getValue().get(k);
                sb.append("    ").append(k).append(": ").append(formatScalar(v)).append('\n');
            }
        }
        return sb.toString();
    }

    private static boolean samePos(ExportBlockPos a, ExportBlockPos b) {
        if (a == null || b == null) {
            return false;
        }
        return a.x() == b.x() && a.y() == b.y() && a.z() == b.z() && Objects.equals(a.worldId(), b.worldId());
    }

    private static String escapeScalar(String s) {
        if (s == null) {
            return "\"\"";
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return "\"\"";
        }
        boolean needsQuotes = t.contains(":") || t.contains("[") || t.contains("#") || t.contains("\\n") || t.contains("\\r") || t.contains("\\t") || t.contains("\"") || t.startsWith("{") || t.startsWith("[") || t.startsWith("-") || t.endsWith(":");
        if (!needsQuotes) {
            return t;
        }
        return "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String formatScalar(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        return escapeScalar(String.valueOf(v));
    }

    private record OffsetEntry(int dx, int dy, int dz, String match) {
    }

    private record Offset(int dx, int dy, int dz) {
    }
}

