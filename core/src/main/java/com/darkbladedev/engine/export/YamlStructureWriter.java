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

        String ports = writePorts(controller, ctx);
        if (!ports.isEmpty()) {
            sb.append(ports);
        }

        String extensions = writeExtensions(controller, ctx);
        if (!extensions.isEmpty()) {
            sb.append(extensions);
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

    private static String writePorts(ExportBlockPos controller, DefaultExportContext ctx) {
        List<Offset> inputs = new ArrayList<>();
        List<Offset> outputs = new ArrayList<>();

        for (Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, String> e : ctx.roles().entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            String role = e.getValue().trim().toLowerCase(Locale.ROOT);
            if (!role.equals("input") && !role.equals("output")) {
                continue;
            }
            int dx = e.getKey().x() - controller.x();
            int dy = e.getKey().y() - controller.y();
            int dz = e.getKey().z() - controller.z();
            Offset off = new Offset(dx, dy, dz);
            if (role.equals("input")) {
                inputs.add(off);
            } else {
                outputs.add(off);
            }
        }

        inputs.sort(Comparator.comparingInt((Offset o) -> o.dy).thenComparingInt(o -> o.dx).thenComparingInt(o -> o.dz));
        outputs.sort(Comparator.comparingInt((Offset o) -> o.dy).thenComparingInt(o -> o.dx).thenComparingInt(o -> o.dz));

        if (inputs.isEmpty() && outputs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ports:\n");

        int idxIn = 0;
        for (Offset o : inputs) {
            idxIn++;
            String id = "port_in_" + idxIn;
            sb.append("  ").append(id).append(":\n");
            sb.append("    direction: input\n");
            sb.append("    type: data\n");
            sb.append("    block: ").append(formatBlockRef(o)).append('\n');
            sb.append("    capabilities: [accept]\n");
        }

        int idxOut = 0;
        for (Offset o : outputs) {
            idxOut++;
            String id = "port_out_" + idxOut;
            sb.append("  ").append(id).append(":\n");
            sb.append("    direction: output\n");
            sb.append("    type: data\n");
            sb.append("    block: ").append(formatBlockRef(o)).append('\n');
            sb.append("    capabilities: [emit]\n");
        }

        return sb.toString();
    }

    private static String writeExtensions(ExportBlockPos controller, DefaultExportContext ctx) {
        Map<String, List<Offset>> extraRoles = new LinkedHashMap<>();
        for (Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, String> e : ctx.roles().entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            String role = e.getValue().trim().toLowerCase(Locale.ROOT);
            if (role.equals("controller") || role.equals("input") || role.equals("output")) {
                continue;
            }
            int dx = e.getKey().x() - controller.x();
            int dy = e.getKey().y() - controller.y();
            int dz = e.getKey().z() - controller.z();
            extraRoles.computeIfAbsent(role, k -> new ArrayList<>()).add(new Offset(dx, dy, dz));
        }

        Map<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> props = ctx.properties();
        List<Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>>> propEntries = new ArrayList<>();
        for (Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> e : props.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            propEntries.add(e);
        }
        propEntries.sort(Comparator
                .comparingInt((Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> e) -> e.getKey().y())
                .thenComparingInt(e -> e.getKey().x())
                .thenComparingInt(e -> e.getKey().z())
        );

        boolean hasExtraRoles = extraRoles.values().stream().anyMatch(list -> list != null && !list.isEmpty());
        boolean hasProps = !propEntries.isEmpty();
        if (!hasExtraRoles && !hasProps) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("extensions:\n");
        sb.append("  mbe-exporter:\n");

        if (hasExtraRoles) {
            sb.append("    roles:\n");
            List<String> roles = new ArrayList<>(extraRoles.keySet());
            roles.sort(String::compareToIgnoreCase);
            for (String role : roles) {
                List<Offset> list = extraRoles.get(role);
                if (list == null || list.isEmpty()) {
                    continue;
                }
                list.sort(Comparator.comparingInt((Offset o) -> o.dy).thenComparingInt(o -> o.dx).thenComparingInt(o -> o.dz));
                sb.append("      ").append(role).append(":\n");
                for (Offset o : list) {
                    sb.append("        - [").append(o.dx).append(", ").append(o.dy).append(", ").append(o.dz).append("]\n");
                }
            }
        }

        if (hasProps) {
            sb.append("    properties:\n");
            for (Map.Entry<com.darkbladedev.engine.api.export.ExportBlockPos, Map<String, Object>> e : propEntries) {
                int dx = e.getKey().x() - controller.x();
                int dy = e.getKey().y() - controller.y();
                int dz = e.getKey().z() - controller.z();

                sb.append("      - offset: [").append(dx).append(", ").append(dy).append(", ").append(dz).append("]\n");
                sb.append("        values:\n");

                List<String> keys = new ArrayList<>(e.getValue().keySet());
                keys.removeIf(k -> k == null || k.isBlank());
                keys.sort(String::compareToIgnoreCase);
                for (String k : keys) {
                    Object v = e.getValue().get(k);
                    sb.append("          ").append(k).append(": ").append(formatScalar(v)).append('\n');
                }
            }
        }

        return sb.toString();
    }

    private static String formatBlockRef(Offset offset) {
        if (offset.dx == 0 && offset.dy == 0 && offset.dz == 0) {
            return "controller";
        }
        return "[" + offset.dx + ", " + offset.dy + ", " + offset.dz + "]";
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
