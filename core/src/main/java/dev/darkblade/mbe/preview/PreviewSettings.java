package dev.darkblade.mbe.preview;

import java.time.Duration;

public record PreviewSettings(int batchSize, int raycastDistance, double maxDistance, Duration timeout) {
}
