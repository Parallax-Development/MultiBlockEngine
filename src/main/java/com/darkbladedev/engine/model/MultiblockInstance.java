package com.darkbladedev.engine.model;

import org.bukkit.Location;

public record MultiblockInstance(
    MultiblockType type,
    Location anchorLocation
) {}
