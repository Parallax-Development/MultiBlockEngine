package com.darkbladedev.engine.port;

import com.darkbladedev.engine.api.port.PortBlockRef;
import com.darkbladedev.engine.api.port.PortDefinition;
import com.darkbladedev.engine.api.port.PortDirection;
import com.darkbladedev.engine.api.port.PortResolutionService;
import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultPortResolutionService implements PortResolutionService {

    @Override
    public Optional<Location> resolveBlock(MultiblockInstance instance, PortBlockRef ref) {
        if (instance == null || ref == null) {
            return Optional.empty();
        }
        Location base = instance.anchorLocation();
        if (base == null || base.getWorld() == null) {
            return Optional.empty();
        }

        if (ref instanceof PortBlockRef.Controller) {
            return Optional.of(base.clone());
        }
        if (ref instanceof PortBlockRef.Offset off) {
            BlockFace facing = instance.facing() == null ? BlockFace.NORTH : instance.facing();
            Vector rotated = rotate(new Vector(off.dx(), off.dy(), off.dz()), facing);
            return Optional.of(base.clone().add(rotated.getBlockX(), rotated.getBlockY(), rotated.getBlockZ()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Location> resolvePort(MultiblockInstance instance, String portId) {
        if (instance == null || portId == null || portId.isBlank()) {
            return Optional.empty();
        }
        if (instance.type() == null || instance.type().ports() == null) {
            return Optional.empty();
        }

        PortDefinition def = instance.type().ports().get(portId);
        if (def == null) {
            return Optional.empty();
        }
        return resolvePort(instance, def);
    }

    @Override
    public Optional<Location> resolvePort(MultiblockInstance instance, PortDefinition port) {
        if (port == null) {
            return Optional.empty();
        }
        return resolveBlock(instance, port.block());
    }

    @Override
    public List<ResolvedPort> resolveAll(MultiblockInstance instance) {
        if (instance == null || instance.type() == null) {
            return List.of();
        }
        Map<String, PortDefinition> ports = instance.type().ports();
        if (ports == null || ports.isEmpty()) {
            return List.of();
        }

        List<ResolvedPort> out = new ArrayList<>();
        for (PortDefinition def : ports.values()) {
            if (def == null) {
                continue;
            }
            Optional<Location> loc = resolvePort(instance, def);
            loc.ifPresent(location -> out.add(new ResolvedPort(def, location)));
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    @Override
    public List<ResolvedPort> resolveByType(MultiblockInstance instance, String type) {
        String t = type == null ? "" : type.trim();
        if (t.isEmpty()) {
            return List.of();
        }
        return resolveFiltered(instance, def -> t.equals(def.type()));
    }

    @Override
    public List<ResolvedPort> resolveByDirection(MultiblockInstance instance, PortDirection direction) {
        if (direction == null) {
            return List.of();
        }
        return resolveFiltered(instance, def -> direction == def.direction());
    }

    @Override
    public List<ResolvedPort> resolveByCapability(MultiblockInstance instance, String capability) {
        String c = capability == null ? "" : capability.trim();
        if (c.isEmpty()) {
            return List.of();
        }
        return resolveFiltered(instance, def -> def.capabilities() != null && def.capabilities().contains(c));
    }

    private interface PortPredicate {
        boolean test(PortDefinition def);
    }

    private List<ResolvedPort> resolveFiltered(MultiblockInstance instance, PortPredicate pred) {
        Objects.requireNonNull(pred, "pred");

        if (instance == null || instance.type() == null) {
            return List.of();
        }
        Map<String, PortDefinition> ports = instance.type().ports();
        if (ports == null || ports.isEmpty()) {
            return List.of();
        }

        List<ResolvedPort> out = new ArrayList<>();
        for (PortDefinition def : ports.values()) {
            if (def == null || !pred.test(def)) {
                continue;
            }
            Optional<Location> loc = resolvePort(instance, def);
            loc.ifPresent(location -> out.add(new ResolvedPort(def, location)));
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static Vector rotate(Vector v, BlockFace facing) {
        Vector vv = v == null ? new Vector(0, 0, 0) : v;
        BlockFace f = facing == null ? BlockFace.NORTH : facing;

        return switch (f) {
            case NORTH -> vv.clone();
            case EAST -> new Vector(-vv.getZ(), vv.getY(), vv.getX());
            case SOUTH -> new Vector(-vv.getX(), vv.getY(), -vv.getZ());
            case WEST -> new Vector(vv.getZ(), vv.getY(), -vv.getX());
            default -> vv.clone();
        };
    }
}
