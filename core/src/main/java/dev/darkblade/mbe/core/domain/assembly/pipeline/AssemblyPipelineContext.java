package dev.darkblade.mbe.core.domain.assembly.pipeline;

import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.core.domain.MultiblockType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class AssemblyPipelineContext {

    private final AssemblyContext assemblyContext;
    private MultiblockCandidate candidate;
    private boolean triggerMatched;
    private boolean controllerMatched;
    private boolean matched;
    private BlockFace matchedFacing;

    public AssemblyPipelineContext(AssemblyContext assemblyContext) {
        this.assemblyContext = assemblyContext;
    }

    public void resetForCandidate(MultiblockCandidate candidate) {
        this.candidate = candidate;
        this.triggerMatched = false;
        this.controllerMatched = false;
        this.matched = false;
        this.matchedFacing = null;
    }

    public AssemblyContext assemblyContext() {
        return assemblyContext;
    }

    public Player player() {
        return assemblyContext == null ? null : assemblyContext.player();
    }

    public Block controller() {
        return candidate == null ? null : candidate.controller();
    }

    public MultiblockType type() {
        return candidate == null ? null : candidate.type();
    }

    public String multiblockId() {
        MultiblockType type = type();
        return type == null || type.id() == null ? "" : type.id();
    }

    public String triggerId() {
        return candidate == null || candidate.triggerId() == null ? "" : candidate.triggerId();
    }

    public boolean forceTrigger() {
        return candidate != null && candidate.forceTrigger();
    }

    public boolean triggerMatched() {
        return triggerMatched;
    }

    public void setTriggerMatched(boolean triggerMatched) {
        this.triggerMatched = triggerMatched;
    }

    public boolean controllerMatched() {
        return controllerMatched;
    }

    public void setControllerMatched(boolean controllerMatched) {
        this.controllerMatched = controllerMatched;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public BlockFace matchedFacing() {
        return matchedFacing;
    }

    public void setMatchedFacing(BlockFace matchedFacing) {
        this.matchedFacing = matchedFacing;
    }
}
