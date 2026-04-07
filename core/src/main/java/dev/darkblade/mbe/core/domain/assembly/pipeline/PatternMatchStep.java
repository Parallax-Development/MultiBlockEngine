package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class PatternMatchStep implements AssemblyStep {

    private final Function<Block, List<BlockFace>> facingCandidates;
    private final TriPredicate<Block, MultiblockType, BlockFace> patternMatches;

    public PatternMatchStep(
            Function<Block, List<BlockFace>> facingCandidates,
            TriPredicate<Block, MultiblockType, BlockFace> patternMatches
    ) {
        this.facingCandidates = Objects.requireNonNull(facingCandidates, "facingCandidates");
        this.patternMatches = Objects.requireNonNull(patternMatches, "patternMatches");
    }

    @Override
    public AssemblyStepResult execute(AssemblyPipelineContext ctx, AssemblyTraceCollector trace) {
        if (!ctx.controllerMatched()) {
            return AssemblyStepResult.continueStep();
        }
        if (ctx.type() == null) {
            return AssemblyStepResult.continueStep();
        }
        Block controller = ctx.controller();
        String multiblockId = ctx.multiblockId();
        List<BlockFace> facings = facingCandidates.apply(controller);
        for (BlockFace facing : facings) {
            if (facing == null) {
                continue;
            }
            boolean matched = patternMatches.test(controller, ctx.type(), facing);
            trace.add(
                    "pattern_match",
                    matched,
                    matched ? "Pattern matched" : "Pattern mismatch",
                    Map.of("multiblockId", multiblockId, "facing", facing.name())
            );
            if (matched) {
                ctx.setMatched(true);
                ctx.setMatchedFacing(facing);
                break;
            }
        }
        return AssemblyStepResult.continueStep();
    }

    @FunctionalInterface
    public interface TriPredicate<T, U, V> {
        boolean test(T t, U u, V v);
    }
}
