package dev.darkblade.mbe.api.service;

public record InspectionEntry(
    String key,
    Object value,
    EntryType type,
    InspectionLevel visibility
) {
}

