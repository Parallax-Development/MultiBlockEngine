package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public record Vector3i(int x, int y, int z) {
    public static Vector3i fromVector(Vector vector) {
        if (vector == null) {
            return new Vector3i(0, 0, 0);
        }
        return new Vector3i(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public Location addTo(Location location) {
        return location.clone().add(x, y, z);
    }
}
