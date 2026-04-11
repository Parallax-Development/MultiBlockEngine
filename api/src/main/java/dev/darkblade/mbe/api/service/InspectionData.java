package dev.darkblade.mbe.api.service;

import java.util.Map;

public record InspectionData(
    Map<String, InspectionEntry> entries
) {
}

