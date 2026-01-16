package com.darkbladedev.engine.inspection;

import com.darkbladedev.engine.api.inspection.Inspectable;
import com.darkbladedev.engine.api.inspection.InspectionContext;
import com.darkbladedev.engine.api.inspection.InspectionData;
import com.darkbladedev.engine.api.inspection.InspectionLevel;
import com.darkbladedev.engine.api.inspection.InspectionPipelineService;
import com.darkbladedev.engine.api.inspection.InspectionRenderer;
import com.darkbladedev.engine.api.inspection.InteractionSource;
import org.bukkit.entity.Player;

import java.util.Map;

public final class DefaultInspectionPipelineService implements InspectionPipelineService {

    @Override
    public void inspect(Player player, InteractionSource source, InspectionLevel requestedLevel, Inspectable inspectable, InspectionRenderer renderer) {
        if (player == null || inspectable == null || renderer == null) {
            return;
        }

        InspectionLevel allowed = resolveAllowedLevel(player);
        InspectionLevel requested = requestedLevel == null ? allowed : requestedLevel;
        InspectionLevel effective = requested.ordinal() > allowed.ordinal() ? allowed : requested;

        InteractionSource safeSource = source == null ? InteractionSource.AUTO : source;
        InspectionContext ctx = new InspectionContext(player, effective, safeSource);

        InspectionData raw = safeInspect(inspectable, ctx);
        InspectionData filtered = InspectionFilters.filterByLevel(raw, effective);
        renderer.render(player, filtered, ctx);
    }

    private static InspectionLevel resolveAllowedLevel(Player player) {
        if (player == null) {
            return InspectionLevel.PLAYER;
        }
        if (player.hasPermission("mbe.inspect.internal")) {
            return InspectionLevel.INTERNAL;
        }
        if (player.hasPermission("mbe.inspect.debug")) {
            return InspectionLevel.DEBUG;
        }
        if (player.isOp() || player.hasPermission("mbe.inspect.operator")) {
            return InspectionLevel.OPERATOR;
        }
        return InspectionLevel.PLAYER;
    }

    private static InspectionData safeInspect(Inspectable inspectable, InspectionContext ctx) {
        try {
            InspectionData data = inspectable.inspect(ctx);
            if (data == null || data.entries() == null || data.entries().isEmpty()) {
                return new InspectionData(Map.of());
            }
            return data;
        } catch (Throwable ignored) {
            return new InspectionData(Map.of());
        }
    }
}

