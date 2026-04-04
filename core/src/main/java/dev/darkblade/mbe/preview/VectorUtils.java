package dev.darkblade.mbe.preview;

public final class VectorUtils {
    private VectorUtils() {
    }

    public static Vector3i rotate(Vector3i vector, Rotation rotation) {
        if (vector == null) {
            return new Vector3i(0, 0, 0);
        }
        if (rotation == null || rotation == Rotation.NORTH) {
            return vector;
        }
        return switch (rotation) {
            case NORTH -> vector;
            case EAST -> new Vector3i(-vector.z(), vector.y(), vector.x());
            case SOUTH -> new Vector3i(-vector.x(), vector.y(), -vector.z());
            case WEST -> new Vector3i(vector.z(), vector.y(), -vector.x());
        };
    }
}
