package dev.darkblade.mbe.core.domain.assembly;

import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.assembly.AssemblyStepTrace;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerRegistry;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerType;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionSource;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.assembly.AssemblyReportService;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;
import dev.darkblade.mbe.core.domain.assembly.pipeline.AssemblyPipelineContext;
import dev.darkblade.mbe.core.domain.assembly.pipeline.AssemblyStep;
import dev.darkblade.mbe.core.domain.assembly.pipeline.AssemblyStepResult;
import dev.darkblade.mbe.core.domain.assembly.pipeline.AssemblyStepResultType;
import dev.darkblade.mbe.core.domain.assembly.pipeline.AssemblyTraceCollector;
import dev.darkblade.mbe.core.domain.assembly.pipeline.ControllerMatchStep;
import dev.darkblade.mbe.core.domain.assembly.pipeline.InstanceCreateStep;
import dev.darkblade.mbe.core.domain.assembly.pipeline.LimitCheckStep;
import dev.darkblade.mbe.core.domain.assembly.pipeline.MultiblockCandidate;
import dev.darkblade.mbe.core.domain.assembly.pipeline.PatternMatchStep;
import dev.darkblade.mbe.core.domain.assembly.pipeline.TriggerCheckStep;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.PatternEntry;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AssemblyCoordinator {

    private static final BlockFace[] ROTATIONS = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    private final MultiblockRuntimeService manager;
    private final AssemblyTriggerRegistry triggers;
    private final CoreLogger log;
    private final AssemblyReportService reportService;
    private final boolean assemblyDebugEnabled;
    private final List<AssemblyStep> pipelineSteps;
    private MultiblockLimitService limitService;

    public AssemblyCoordinator(
            MultiblockRuntimeService manager,
            AssemblyTriggerRegistry triggers,
            AssemblyReportService reportService,
            CoreLogger log,
            boolean assemblyDebugEnabled
    ) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.triggers = Objects.requireNonNull(triggers, "triggers");
        this.reportService = Objects.requireNonNull(reportService, "reportService");
        this.log = Objects.requireNonNull(log, "log");
        this.assemblyDebugEnabled = assemblyDebugEnabled;
        this.pipelineSteps = List.of(
                new TriggerCheckStep(this.triggers),
                new ControllerMatchStep(),
                new PatternMatchStep(this::facingCandidates, this::patternMatches),
                new LimitCheckStep(() -> this.limitService),
                new InstanceCreateStep(this.manager, () -> this.limitService)
        );
        int triggerCount = triggers.all().size();
        if (triggerCount <= 0) {
            log.error("Assembly coordinator initialized without triggers");
        } else {
            log.debug("Assembly coordinator ready", LogKv.kv("triggers", triggerCount));
        }
    }

    public Optional<AssemblyReport> lastReport(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return reportService.get(playerId);
    }

    public void setLimitService(MultiblockLimitService limitService) {
        this.limitService = limitService;
    }

    public AssemblyReport tryAssemble(InteractionIntent intent) {
        AssemblyTraceCollector trace = new AssemblyTraceCollector();
        if (intent == null || intent.targetBlock() == null) {
            trace.add("interaction_target", false, "No interaction target", Map.of());
            return finalizeReport(null, AssemblyReport.fail("no_interaction_target", Map.of(), trace.getTrace()), null);
        }
        trace.add("interaction_target", true, "Interaction target resolved", Map.of(
                "x", intent.targetBlock().getX(),
                "y", intent.targetBlock().getY(),
                "z", intent.targetBlock().getZ()
        ));
        AssemblyContext context = new AssemblyContext(intent.player(), intent.targetBlock(), intent);
        return tryAssembleAtInternal(intent.targetBlock(), context, trace);
    }

    public AssemblyReport attemptAssembly(AssemblyContext context) {
        AssemblyTraceCollector trace = new AssemblyTraceCollector();
        AssemblyContext safeContext = ensureContext(context);
        Block controller = safeContext.origin();
        if (controller == null) {
            trace.add("controller_check", false, "No controller block", Map.of());
            return finalizeReport(safeContext, AssemblyReport.fail("no_controller_block", Map.of(), trace.getTrace()), null);
        }
        trace.add("controller_check", true, "Controller block resolved", Map.of(
                "x", controller.getX(),
                "y", controller.getY(),
                "z", controller.getZ()
        ));
        Optional<MultiblockInstance> existing = manager.getInstanceAt(controller.getLocation());
        if (existing.isPresent()) {
            String multiblockId = existing.get().type().id();
            trace.add("existing_instance", false, "Instance already exists", Map.of("multiblockId", multiblockId));
            return finalizeReport(
                    safeContext,
                    AssemblyReport.fail("instance_exists", Map.of("multiblockId", multiblockId), trace.getTrace()),
                    controller.getLocation()
            );
        }
        List<MultiblockCandidate> candidates = new ArrayList<>();
        for (MultiblockType type : manager.getTypesDeterministic()) {
            if (type != null) {
                candidates.add(new MultiblockCandidate(type, controller, resolveTriggerId(type), true));
            }
        }
        return executePipeline(safeContext, candidates, trace, controller.getLocation());
    }

    public AssemblyReport tryAssembleAt(Block controller, AssemblyContext context) {
        return tryAssembleAtInternal(controller, context, new AssemblyTraceCollector());
    }

    private AssemblyReport tryAssembleAtInternal(Block controller, AssemblyContext context, AssemblyTraceCollector trace) {
        if (controller == null) {
            trace.add("controller_check", false, "No controller block", Map.of());
            return finalizeReport(context, AssemblyReport.fail("no_controller_block", Map.of(), trace.getTrace()), null);
        }
        trace.add("controller_check", true, "Controller block resolved", Map.of(
                "x", controller.getX(),
                "y", controller.getY(),
                "z", controller.getZ()
        ));

        Optional<MultiblockInstance> existing = manager.getInstanceAt(controller.getLocation());
        if (existing.isPresent()) {
            String multiblockId = existing.get().type().id();
            trace.add("existing_instance", false, "Instance already exists", Map.of("multiblockId", multiblockId));
            return finalizeReport(
                    context,
                    AssemblyReport.fail("instance_exists", Map.of("multiblockId", multiblockId), trace.getTrace()),
                    controller.getLocation()
            );
        }

        AssemblyContext safeContext = ensureContext(context);
        List<MultiblockCandidate> candidates = new ArrayList<>();
        for (MultiblockType type : manager.getTypesDeterministic()) {
            if (type != null) {
                candidates.add(new MultiblockCandidate(type, controller, resolveTriggerId(type), false));
            }
        }
        return executePipeline(safeContext, candidates, trace, controller.getLocation());
    }

    public AssemblyReport tryAssembleTypeAt(MultiblockType type, Block controller, AssemblyContext context) {
        AssemblyTraceCollector trace = new AssemblyTraceCollector();
        Objects.requireNonNull(type, "type");
        if (controller == null) {
            trace.add("controller_check", false, "No controller block", Map.of("multiblockId", type.id()));
            return finalizeReport(context, AssemblyReport.fail("no_controller_block", Map.of("multiblockId", type.id()), trace.getTrace()), null);
        }
        AssemblyContext safeContext = ensureContext(context);
        List<MultiblockCandidate> candidates = List.of(new MultiblockCandidate(type, controller, resolveTriggerId(type), false));
        return executePipeline(safeContext, candidates, trace, controller.getLocation());
    }

    public AssemblyReport tryAssembleFromPlacedBlock(Block placedBlock, AssemblyContext context) {
        AssemblyTraceCollector trace = new AssemblyTraceCollector();
        if (placedBlock == null) {
            trace.add("placed_block_check", false, "No placed block", Map.of());
            return finalizeReport(context, AssemblyReport.fail("no_placed_block", Map.of(), trace.getTrace()), null);
        }

        AssemblyContext safeContext = ensureContext(context);
        String requiredTrigger = AssemblyTriggerType.ON_FINAL_BLOCK_PLACED.id();
        trace.add("trigger_requirement", true, "Required trigger selected", Map.of("trigger", requiredTrigger));
        List<MultiblockCandidate> candidates = new ArrayList<>();
        for (MultiblockType type : manager.getTypesDeterministic()) {
            if (type == null) {
                continue;
            }
            String triggerId = resolveTriggerId(type);
            if (!normalize(requiredTrigger).equals(triggerId)) {
                continue;
            }
            Set<Location> controllers = candidateControllersForPlacedBlock(placedBlock, type);
            for (Location loc : controllers) {
                if (loc != null) {
                    candidates.add(new MultiblockCandidate(type, loc.getBlock(), triggerId, false));
                }
            }
        }
        return executePipeline(safeContext, candidates, trace, placedBlock.getLocation());
    }

    private AssemblyReport executePipeline(
            AssemblyContext context,
            List<MultiblockCandidate> candidates,
            AssemblyTraceCollector trace,
            Location fallbackLocation
    ) {
        AssemblyPipelineContext pipelineContext = new AssemblyPipelineContext(context);
        for (MultiblockCandidate candidate : candidates) {
            if (candidate == null || candidate.type() == null || candidate.controller() == null) {
                continue;
            }
            pipelineContext.resetForCandidate(candidate);
            trace.add("candidate_select", true, "Evaluating candidate", Map.of(
                    "multiblockId", candidate.type().id(),
                    "trigger", candidate.triggerId()
            ));
            for (AssemblyStep step : pipelineSteps) {
                AssemblyStepResult result = step.execute(pipelineContext, trace);
                if (result.type() == AssemblyStepResultType.CONTINUE) {
                    continue;
                }
                if (result.type() == AssemblyStepResultType.FAIL) {
                    String reason = result.reasonKey() == null ? "assembly_failed" : result.reasonKey();
                    AssemblyReport report = AssemblyReport.fail(reason, result.data(), trace.getTrace());
                    return finalizeReport(context, report, candidate.controller().getLocation());
                }
                if (result.type() == AssemblyStepResultType.SUCCESS) {
                    AssemblyReport report = AssemblyReport.success(trace.getTrace());
                    return finalizeReport(context, report, candidate.controller().getLocation());
                }
            }
        }
        trace.add("pipeline_end", false, "No multiblock matched", Map.of());
        AssemblyReport report = AssemblyReport.fail("no_multiblock_matched", Map.of(), trace.getTrace());
        return finalizeReport(context, report, fallbackLocation);
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
        Set<Location> out = new LinkedHashSet<>();
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

    private AssemblyContext ensureContext(AssemblyContext context) {
        if (context == null) {
            InteractionIntent manualIntent = new InteractionIntent(
                    null,
                    InteractionType.PROGRAMMATIC,
                    null,
                    null,
                    InteractionSource.PROGRAMMATIC
            );
            return new AssemblyContext(null, null, manualIntent);
        }
        return context;
    }

    private AssemblyReport finalizeReport(AssemblyContext context, AssemblyReport report, Location controller) {
        if (context != null && context.player() != null && report != null) {
            reportService.store(context.player().getUniqueId(), report);
        }
        logDebug(report, controller, context);
        return report;
    }

    private void logDebug(AssemblyReport report, Location controller, AssemblyContext context) {
        if (report == null) {
            return;
        }
        log.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.DEBUG, "Assembly attempt", null, new LogKv[] {
                LogKv.kv("success", report.success()),
                LogKv.kv("trigger", report.trigger()),
                LogKv.kv("multiblock", report.multiblockId()),
                LogKv.kv("reason", report.reasonKey()),
                LogKv.kv("x", controller != null ? controller.getBlockX() : 0),
                LogKv.kv("y", controller != null ? controller.getBlockY() : 0),
                LogKv.kv("z", controller != null ? controller.getBlockZ() : 0)
        }, Set.of("assembly"));
        if (!report.success()) {
            log.warn(
                    "assembly.failed",
                    LogKv.kv("reason", report.reasonKey() == null ? "" : report.reasonKey()),
                    LogKv.kv("player", context != null && context.player() != null ? context.player().getName() : "console")
            );
        }
        if (assemblyDebugEnabled) {
            int index = 1;
            for (AssemblyStepTrace step : report.trace()) {
                if (step == null) {
                    continue;
                }
                log.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.DEBUG, "Assembly trace step", null, new LogKv[] {
                        LogKv.kv("index", index++),
                        LogKv.kv("step", step.step()),
                        LogKv.kv("success", step.success()),
                        LogKv.kv("detail", step.detail()),
                        LogKv.kv("data", step.data())
                }, Set.of("assembly", "trace"));
            }
        }
    }

    private String normalize(String id) {
        return (id == null ? "" : id.trim()).toLowerCase(Locale.ROOT);
    }

    private String resolveTriggerId(MultiblockType type) {
        if (type == null) {
            return "";
        }
        String triggerId = normalize(type.assemblyTrigger());
        if (triggerId.isBlank()) {
            return normalize(AssemblyTriggerType.WRENCH_USE.id());
        }
        return triggerId;
    }
}
