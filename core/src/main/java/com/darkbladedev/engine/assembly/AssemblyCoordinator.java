package com.darkbladedev.engine.assembly;

import com.darkbladedev.engine.api.assembly.AssemblyContext;
import com.darkbladedev.engine.api.assembly.AssemblyReport;
import com.darkbladedev.engine.api.assembly.AssemblyTrigger;
import com.darkbladedev.engine.api.assembly.AssemblyTriggerRegistry;
import com.darkbladedev.engine.api.assembly.AssemblyTriggerType;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AssemblyCoordinator {

    private static final BlockFace[] ROTATIONS = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    private final MultiblockManager manager;
    private final AssemblyTriggerRegistry triggers;
    private final CoreLogger log;
    private final Map<UUID, AssemblyReport> lastReportByPlayer = new ConcurrentHashMap<>();

    public AssemblyCoordinator(MultiblockManager manager, AssemblyTriggerRegistry triggers, CoreLogger log) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.triggers = Objects.requireNonNull(triggers, "triggers");
        this.log = Objects.requireNonNull(log, "log");
    }

    public Optional<AssemblyReport> lastReport(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastReportByPlayer.get(playerId));
    }

    public AssemblyReport tryAssembleAt(Block controller, AssemblyContext context) {
        if (controller == null) {
            return report(context, "", false, false, AssemblyReport.MatcherResult.SKIPPED, AssemblyReport.Result.FAILED, "", "No controller block");
        }

        Optional<MultiblockInstance> existing = manager.getInstanceAt(controller.getLocation());
        if (existing.isPresent()) {
            return report(context, "", true, true, AssemblyReport.MatcherResult.SKIPPED, AssemblyReport.Result.ABORTED, existing.get().type().id(), "Instance already exists");
        }

        AssemblyContext safeContext = ensureAttributes(context);

        for (MultiblockType type : manager.getTypesDeterministic()) {
            if (type == null) {
                continue;
            }
            if (!type.controllerMatcher().matches(controller)) {
                continue;
            }

            AssemblyAttempt attempt = tryAssembleTypeAtInternal(type, controller, safeContext);
            if (attempt.report().result() == AssemblyReport.Result.SUCCESS) {
                remember(safeContext, attempt.report());
                logDebug(attempt.report(), controller.getLocation());
                return attempt.report();
            }
        }

        AssemblyReport out = report(safeContext, "", true, true, AssemblyReport.MatcherResult.MISMATCH, AssemblyReport.Result.FAILED, "", "No multiblock matched");
        remember(safeContext, out);
        logDebug(out, controller.getLocation());
        return out;
    }

    public AssemblyReport tryAssembleTypeAt(MultiblockType type, Block controller, AssemblyContext context) {
        Objects.requireNonNull(type, "type");
        if (controller == null) {
            return report(context, type.assemblyTrigger(), false, false, AssemblyReport.MatcherResult.SKIPPED, AssemblyReport.Result.FAILED, type.id(), "No controller block");
        }
        AssemblyContext safeContext = ensureAttributes(context);
        AssemblyAttempt attempt = tryAssembleTypeAtInternal(type, controller, safeContext);
        remember(safeContext, attempt.report());
        logDebug(attempt.report(), controller.getLocation());
        return attempt.report();
    }

    public AssemblyReport tryAssembleFromPlacedBlock(Block placedBlock, AssemblyContext context) {
        if (placedBlock == null) {
            return report(context, "", false, false, AssemblyReport.MatcherResult.SKIPPED, AssemblyReport.Result.FAILED, "", "No placed block");
        }

        AssemblyContext safeContext = ensureAttributes(context);
        String requiredTrigger = AssemblyTriggerType.ON_FINAL_BLOCK_PLACED.id();

        for (MultiblockType type : manager.getTypesDeterministic()) {
            if (type == null) {
                continue;
            }
            String triggerId = normalize(type.assemblyTrigger());
            if (!normalize(requiredTrigger).equals(triggerId)) {
                continue;
            }

            AssemblyTrigger trigger = triggers.get(triggerId).orElse(null);
            if (trigger == null || !trigger.shouldTrigger(safeContext)) {
                continue;
            }

            Set<Location> controllers = candidateControllersForPlacedBlock(placedBlock, type);
            for (Location loc : controllers) {
                if (loc == null) {
                    continue;
                }
                Block controller = loc.getBlock();
                AssemblyAttempt attempt = tryAssembleTypeAtInternal(type, controller, safeContext);
                if (attempt.report().result() == AssemblyReport.Result.SUCCESS) {
                    remember(safeContext, attempt.report());
                    logDebug(attempt.report(), controller.getLocation());
                    return attempt.report();
                }
            }
        }

        AssemblyReport out = report(safeContext, requiredTrigger, true, true, AssemblyReport.MatcherResult.MISMATCH, AssemblyReport.Result.FAILED, "", "No multiblock matched");
        remember(safeContext, out);
        logDebug(out, placedBlock.getLocation());
        return out;
    }

    private AssemblyAttempt tryAssembleTypeAtInternal(MultiblockType type, Block controller, AssemblyContext context) {
        String triggerId = normalize(type.assemblyTrigger());
        if (triggerId.isBlank()) {
            triggerId = normalize(AssemblyTriggerType.WRENCH_USE.id());
        }

        AssemblyTrigger trigger = triggers.get(triggerId).orElse(null);
        if (trigger == null) {
            return new AssemblyAttempt(Optional.empty(), report(context, triggerId, false, true, AssemblyReport.MatcherResult.SKIPPED, AssemblyReport.Result.ABORTED, type.id(), "Unknown trigger"));
        }

        boolean should = trigger.shouldTrigger(context);
        if (!should) {
            return new AssemblyAttempt(Optional.empty(), report(context, triggerId, false, true, AssemblyReport.MatcherResult.SKIPPED, AssemblyReport.Result.ABORTED, type.id(), "Trigger not matched"));
        }

        if (!type.controllerMatcher().matches(controller)) {
            return new AssemblyAttempt(Optional.empty(), report(context, triggerId, true, true, AssemblyReport.MatcherResult.MISMATCH, AssemblyReport.Result.FAILED, type.id(), "Controller mismatch"));
        }

        List<BlockFace> facings = facingCandidates(controller);
        boolean matched = false;
        for (BlockFace facing : facings) {
            if (facing == null) {
                continue;
            }
            if (patternMatches(controller, type, facing)) {
                matched = true;
                Optional<MultiblockInstance> created = manager.tryCreate(controller, type, context.player());
                if (created.isPresent()) {
                    return new AssemblyAttempt(created, report(context, triggerId, true, true, AssemblyReport.MatcherResult.MATCH, AssemblyReport.Result.SUCCESS, type.id(), ""));
                }
                return new AssemblyAttempt(Optional.empty(), report(context, triggerId, true, true, AssemblyReport.MatcherResult.MATCH, AssemblyReport.Result.ABORTED, type.id(), "Creation cancelled"));
            }
        }

        return new AssemblyAttempt(Optional.empty(), report(context, triggerId, true, true, matched ? AssemblyReport.MatcherResult.MATCH : AssemblyReport.MatcherResult.MISMATCH, AssemblyReport.Result.FAILED, type.id(), "Pattern mismatch"));
    }

    private List<BlockFace> facingCandidates(Block controller) {
        List<BlockFace> candidates = new ArrayList<>();
        if (controller.getBlockData() instanceof Directional d) {
            candidates.add(d.getFacing());
        } else {
            for (BlockFace f : ROTATIONS) {
                candidates.add(f);
            }
        }
        return candidates;
    }

    private boolean patternMatches(Block controller, MultiblockType type, BlockFace facing) {
        for (PatternEntry entry : type.pattern()) {
            Vector rotatedOffset = rotate(entry.offset(), facing);
            Block target = controller.getRelative(rotatedOffset.getBlockX(), rotatedOffset.getBlockY(), rotatedOffset.getBlockZ());
            if (!target.getChunk().isLoaded()) {
                return false;
            }
            if (!entry.matcher().matches(target)) {
                if (!entry.optional()) {
                    return false;
                }
            }
        }
        return true;
    }

    private Vector rotate(Vector v, BlockFace facing) {
        if (v == null) {
            return new Vector(0, 0, 0);
        }
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        return switch (facing) {
            case NORTH -> new Vector(x, y, z);
            case EAST -> new Vector(-z, y, x);
            case SOUTH -> new Vector(-x, y, -z);
            case WEST -> new Vector(z, y, -x);
            default -> new Vector(x, y, z);
        };
    }

    private Set<Location> candidateControllersForPlacedBlock(Block placedBlock, MultiblockType type) {
        Set<Location> out = new HashSet<>();
        Location placed = placedBlock.getLocation();

        if (type.controllerMatcher().matches(placedBlock)) {
            out.add(placed);
        }

        for (BlockFace facing : ROTATIONS) {
            for (PatternEntry entry : type.pattern()) {
                Vector rotatedOffset = rotate(entry.offset(), facing);
                Location candidate = placed.clone().subtract(rotatedOffset);
                out.add(candidate);
            }
        }

        return out;
    }

    private AssemblyReport report(AssemblyContext context, String trigger, boolean triggerMatched, boolean controllerFound, AssemblyReport.MatcherResult matcher, AssemblyReport.Result result, String multiblockId, String reason) {
        return new AssemblyReport(
                trigger == null ? "" : trigger,
                triggerMatched,
                controllerFound,
                matcher == null ? AssemblyReport.MatcherResult.SKIPPED : matcher,
                List.of(),
                result == null ? AssemblyReport.Result.FAILED : result,
                multiblockId == null ? "" : multiblockId,
                reason == null ? "" : reason
        );
    }

    private AssemblyContext ensureAttributes(AssemblyContext context) {
        if (context == null) {
            return new AssemblyContext(AssemblyContext.Cause.MANUAL, null, null, null, null, null, false, Map.of());
        }
        Map<String, Object> map = context.attributes() == null ? Map.of() : context.attributes();
        return new AssemblyContext(
                context.cause(),
                context.player(),
                context.block(),
                context.action(),
                context.item(),
                context.hand(),
                context.sneaking(),
                map
        );
    }

    private void remember(AssemblyContext context, AssemblyReport report) {
        if (context == null || context.player() == null) {
            return;
        }
        lastReportByPlayer.put(context.player().getUniqueId(), report);
    }

    private void logDebug(AssemblyReport report, Location controller) {
        if (report == null) {
            return;
        }
        log.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.DEBUG, "Assembly attempt", null, new LogKv[] {
                LogKv.kv("result", report.result().name()),
                LogKv.kv("trigger", report.trigger()),
                LogKv.kv("multiblock", report.multiblockId()),
                LogKv.kv("reason", report.failureReason()),
                LogKv.kv("x", controller != null ? controller.getBlockX() : 0),
                LogKv.kv("y", controller != null ? controller.getBlockY() : 0),
                LogKv.kv("z", controller != null ? controller.getBlockZ() : 0)
        }, Set.of("assembly"));
    }

    private String normalize(String id) {
        return (id == null ? "" : id.trim()).toLowerCase(Locale.ROOT);
    }

    private record AssemblyAttempt(Optional<MultiblockInstance> instance, AssemblyReport report) {
        private AssemblyAttempt {
            instance = instance == null ? Optional.empty() : instance;
            report = Objects.requireNonNull(report, "report");
        }
    }
}
