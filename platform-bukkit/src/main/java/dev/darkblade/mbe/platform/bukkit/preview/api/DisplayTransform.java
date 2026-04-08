package dev.darkblade.mbe.platform.bukkit.preview.api;

public record DisplayTransform(
    float translationX,
    float translationY,
    float translationZ,
    float scaleX,
    float scaleY,
    float scaleZ,
    int interpolationDuration,
    int startInterpolation
) {
    public static DisplayTransform identity() {
        return new DisplayTransform(0F, 0F, 0F, 1F, 1F, 1F, 0, 0);
    }
}
