package com.darkbladedev.engine.inspection;

import com.darkbladedev.engine.api.inspection.InspectionData;
import com.darkbladedev.engine.api.inspection.InspectionEntry;
import com.darkbladedev.engine.api.inspection.InspectionLevel;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InspectionFilters {

    public static InspectionData filterByLevel(InspectionData data, InspectionLevel allowed) {
        if (data == null || data.entries() == null || data.entries().isEmpty()) {
            return new InspectionData(Map.of());
        }

        InspectionLevel a = allowed == null ? InspectionLevel.PLAYER : allowed;
        Map<String, InspectionEntry> out = new LinkedHashMap<>();
        for (Map.Entry<String, InspectionEntry> e : data.entries().entrySet()) {
            if (e == null) {
                continue;
            }
            String k = e.getKey();
            InspectionEntry v = e.getValue();
            if (k == null || v == null) {
                continue;
            }
            InspectionLevel vis = v.visibility() == null ? InspectionLevel.PLAYER : v.visibility();
            if (vis == InspectionLevel.INTERNAL) {
                continue;
            }
            if (vis.ordinal() > a.ordinal()) {
                continue;
            }
            out.put(k, v);
        }

        return new InspectionData(Map.copyOf(out));
    }

    private InspectionFilters() {
    }
}

