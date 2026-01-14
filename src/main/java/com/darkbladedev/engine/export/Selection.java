package com.darkbladedev.engine.export;

import org.bukkit.World;

public record Selection(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeY() {
        return maxY - minY + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }
}

