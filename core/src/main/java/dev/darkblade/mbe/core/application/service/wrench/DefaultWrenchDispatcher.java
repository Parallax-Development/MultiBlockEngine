package dev.darkblade.mbe.core.application.service.wrench;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.service.EntryType;
import dev.darkblade.mbe.api.service.Inspectable;
import dev.darkblade.mbe.api.service.InspectionData;
import dev.darkblade.mbe.api.service.InspectionEntry;
import dev.darkblade.mbe.api.service.InspectionLevel;
import dev.darkblade.mbe.api.service.InspectionPipelineService;
import dev.darkblade.mbe.api.service.InspectionRenderer;
import dev.darkblade.mbe.api.service.InteractionSource;
import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.api.command.WrenchInteractable;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.assembly.AssemblyStepTrace;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerType;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.BlockMatcher;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockInteractEvent;
import dev.darkblade.mbe.core.domain.rule.AirMatcher;
import dev.darkblade.mbe.core.domain.rule.AnyOfMatcher;
import dev.darkblade.mbe.core.domain.rule.BlockDataMatcher;
import dev.darkblade.mbe.core.domain.rule.ExactMaterialMatcher;
import dev.darkblade.mbe.core.domain.rule.TagMatcher;
import dev.darkblade.mbe.core.domain.interaction.pipeline.WrenchPipelineStage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DefaultWrenchDispatcher implements WrenchDispatcher {

    public static final ItemKey WRENCH_KEY = ItemKeys.of("mbe:wrench", 0);

    private static final String ORIGIN = "mbe";
    private static final String MSG_ASSEMBLED = "core.wrench.assembled";
    private static final String MSG_DISASSEMBLED = "core.wrench.disassembled";
    private static final String MSG_COOLDOWN = "core.wrench.cooldown";
    private static final String MSG_MISSING = "core.wrench.missing";
    private static final String MSG_NOT_FOUND = "core.wrench.not_found";
    private static final String MSG_INTERACTION_DENIED = "core.wrench.interaction.denied";
    private static final MessageKey MSG_INSPECT_TITLE = MessageKey.of(ORIGIN, "core.wrench.inspect.title");
    private static final MessageKey MSG_INSPECT_TYPE = MessageKey.of(ORIGIN, "core.wrench.inspect.type");
    private static final MessageKey MSG_INSPECT_STATE = MessageKey.of(ORIGIN, "core.wrench.inspect.state");
    private static final MessageKey MSG_INSPECT_FACING = MessageKey.of(ORIGIN, "core.wrench.inspect.facing");
    private static final MessageKey MSG_INSPECT_ANCHOR = MessageKey.of(ORIGIN, "core.wrench.inspect.anchor");
    private static final MessageKey MSG_INSPECT_COMPONENTS = MessageKey.of(ORIGIN, "core.wrench.inspect.components");
    private static final Set<Material> TILLABLE_SOIL = Set.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT
    );

    private final MultiblockRuntimeService manager;
    private final ItemStackBridge itemStackBridge;
    private final I18nService i18n;
    private final Consumer<Event> eventCaller;
    private final AssemblyCoordinator assembly;
    private final Map<String, WrenchInteractable> actions = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Instant> cooldowns = new ConcurrentHashMap<>();
    private final List<WrenchPipelineStage> pipeline = new ArrayList<>();
    private final WrenchConditionService conditionService = new WrenchConditionService();

    private static final Duration COOLDOWN = Duration.ofMillis(500);
    private WrenchInteractable resolvedInteractable;

    public DefaultWrenchDispatcher(MultiblockRuntimeService manager, ItemStackBridge itemStackBridge, I18nService i18n) {
        this(manager, itemStackBridge, i18n, null, Bukkit.getPluginManager()::callEvent);
    }

    public DefaultWrenchDispatcher(MultiblockRuntimeService manager, ItemStackBridge itemStackBridge, I18nService i18n, AssemblyCoordinator assembly) {
        this(manager, itemStackBridge, i18n, assembly, Bukkit.getPluginManager()::callEvent);
    }

    public DefaultWrenchDispatcher(MultiblockRuntimeService manager, ItemStackBridge itemStackBridge, I18nService i18n, Consumer<Event> eventCaller) {
        this(manager, itemStackBridge, i18n, null, eventCaller);
    }

    public DefaultWrenchDispatcher(MultiblockRuntimeService manager, ItemStackBridge itemStackBridge, I18nService i18n, AssemblyCoordinator assembly, Consumer<Event> eventCaller) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.itemStackBridge = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
        this.i18n = i18n;
        this.assembly = assembly;
        this.eventCaller = Objects.requireNonNull(eventCaller, "eventCaller");
        this.pipeline.add(new PreValidationStage());
        this.pipeline.add(new ConditionEvaluationStage());
        this.pipeline.add(new InteractionResolutionStage());
        this.pipeline.add(new ExecutionStage());
    }

    @Override
    public void registerAction(String key, WrenchInteractable interactable) {
        WrenchDispatcher.requireNamespacedKey(key);
        Objects.requireNonNull(interactable, "interactable");
        String normalized = WrenchDispatcher.normalizeKey(key);
        actions.put(normalized, interactable);
    }

    @Override
    public WrenchResult dispatch(WrenchContext context) {
        Objects.requireNonNull(context, "context");
        Block block = context.clickedBlock();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(block.getLocation());
        if (!isWrench(context.item())) {
            return handleNonWrenchInteraction(context, instanceOpt.orElse(null));
        }
        try {
            for (WrenchPipelineStage stage : pipeline) {
                debugStage(stage);
                WrenchResult result = safeProcessStage(stage, context);
                debugResult(result);
                if (!result.isPass()) {
                    handleResult(context.player(), result);
                    incrementWrenchInteractionMetric();
                    return result;
                }
            }
        } finally {
            resolvedInteractable = null;
        }
        return WrenchResult.pass();
    }

    private WrenchResult safeProcessStage(WrenchPipelineStage stage, WrenchContext context) {
        try {
            WrenchResult result = stage.process(context);
            return result == null ? WrenchResult.pass() : result;
        } catch (Throwable t) {
            safeLogAddonActionError(t);
            return WrenchResult.fail(MSG_INTERACTION_DENIED);
        }
    }

    private WrenchResult handleNonWrenchInteraction(WrenchContext context, MultiblockInstance instance) {
        if (instance == null || context.action() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return WrenchResult.pass();
        }
        boolean cancelled = runInteractActions(instance, context.player(), context.clickedBlock(), context.action());
        return cancelled ? WrenchResult.success(null) : WrenchResult.pass();
    }

    private WrenchResult dispatchAddonActions(WrenchContext context) {
        if (actions.isEmpty()) {
            return WrenchResult.pass();
        }
        Map<String, WrenchInteractable> snapshot = new LinkedHashMap<>(actions);
        for (WrenchInteractable interactable : snapshot.values()) {
            if (interactable == null) {
                continue;
            }
            WrenchResult r;
            try {
                r = interactable.onWrenchUse(context);
            } catch (Throwable t) {
                safeLogAddonActionError(t);
                continue;
            }
            if (r != null && !r.isPass()) {
                return r;
            }
        }
        return WrenchResult.pass();
    }

    private WrenchInteractable resolveBuiltInInteractable(WrenchContext context) {
        Block block = context.clickedBlock();
        Player player = context.player();
        org.bukkit.event.block.Action action = context.action();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(block.getLocation());
        if (instanceOpt.isPresent()) {
            MultiblockInstance instance = instanceOpt.get();
            if (player != null && action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
                return ctx -> {
                    inspect(player, instance);
                    playInspect(player, block.getLocation());
                    return WrenchResult.success(null);
                };
            }
            if (player != null && action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK && player.isSneaking() && isAnchorBlock(block, instance)) {
                return ctx -> {
                    Optional<MultiblockInstance> switched = manager.switchVariant(instance, player);
                    if (switched.isPresent()) {
                        playSuccess(player, block.getLocation());
                        return WrenchResult.success(null);
                    }
                    playFailure(player, block.getLocation());
                    return WrenchResult.fail(MSG_NOT_FOUND);
                };
            }
            if (action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK && isAnchorBlock(block, instance)) {
                return ctx -> disassembleInteractable(ctx, instance);
            }
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                return ctx -> runInteractActions(instance, player, block, action) ? WrenchResult.success(null) : WrenchResult.pass();
            }
            return null;
        }
        if (action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            return ctx -> WrenchResult.fail(MSG_NOT_FOUND);
        }
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return null;
        }
        return ctx -> assembleOrFeedback(ctx);
    }

    private WrenchResult assembleOrFeedback(WrenchContext context) {
        Block block = context.clickedBlock();
        Player player = context.player();
        AssemblyReport assembled = tryAssemble(block, context);
        if (assembled != null && assembled.result() == AssemblyReport.Result.SUCCESS && assembled.multiblockId() != null && !assembled.multiblockId().isBlank()) {
            playSuccess(player, block.getLocation());
            return WrenchResult.success(MSG_ASSEMBLED, Map.of("type", assembled.multiblockId()));
        }
        String missing = formatMissingForBestMatch(block);
        if (!missing.isBlank()) {
            playFailure(player, block.getLocation());
            return WrenchResult.fail(MSG_MISSING, Map.of("missing", missing));
        }
        if (isControllerBlock(block) || isTillableSoil(block)) {
            return WrenchResult.success(null);
        }
        return WrenchResult.pass();
    }

    private WrenchResult disassembleInteractable(WrenchContext context, MultiblockInstance instance) {
        Player player = context.player();
        if (instance == null) {
            return WrenchResult.fail(MSG_NOT_FOUND);
        }
        boolean destroyed = disassemble(player, instance);
        if (!destroyed) {
            return WrenchResult.fail(MSG_NOT_FOUND);
        }
        playDisassemble(player, context.clickedBlock().getLocation());
        return WrenchResult.success(MSG_DISASSEMBLED, Map.of("type", safe(instance.type().id())));
    }

    private boolean runInteractActions(MultiblockInstance instance, Player player, Block block, org.bukkit.event.block.Action action) {
        MultiblockInteractEvent mbEvent = new MultiblockInteractEvent(instance, player, action, block);
        eventCaller.accept(mbEvent);
        if (mbEvent.isCancelled()) {
            return true;
        }
        boolean cancelVanilla = false;
        for (Action a : instance.type().onInteractActions()) {
            if (a != null && a.shouldExecuteOnInteract(action)) {
                if (a.cancelsVanillaOnInteract(action)) {
                    cancelVanilla = true;
                }
                executeActionSafely("INTERACT", a, instance, player);
            }
        }
        return cancelVanilla;
    }

    private void handleResult(Player player, WrenchResult result) {
        if (result == null || result.isPass()) {
            return;
        }
        if (!result.isSuccess() && !result.isFail()) {
            return;
        }
        if (result.messageKey() == null || result.messageKey().isBlank()) {
            return;
        }
        send(player, result.messageKey(), result.context());
    }

    private void incrementWrenchInteractionMetric() {
        try {
            manager.getMetrics().increment("wrench.interactions");
        } catch (Throwable ignored) {
        }
    }

    private void debugStage(WrenchPipelineStage stage) {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        CoreLogger core = plugin != null && plugin.getLoggingService() != null ? plugin.getLoggingService().core() : null;
        if (core == null) {
            return;
        }
        core.debug(
                "Wrench stage",
                LogKv.kv("stage", stage == null ? "unknown" : stage.getClass().getSimpleName())
        );
    }

    private void debugResult(WrenchResult result) {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        CoreLogger core = plugin != null && plugin.getLoggingService() != null ? plugin.getLoggingService().core() : null;
        if (core == null || result == null) {
            return;
        }
        core.debug(
                "Wrench result",
                LogKv.kv("type", result.type().name()),
                LogKv.kv("messageKey", result.messageKey() == null ? "" : result.messageKey())
        );
    }

    private final class PreValidationStage implements WrenchPipelineStage {
        @Override
        public WrenchResult process(WrenchContext context) {
            if (context == null || context.clickedBlock() == null || context.player() == null) {
                return WrenchResult.pass();
            }
            org.bukkit.event.block.Action action = context.action();
            if (action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && action != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
                return WrenchResult.pass();
            }
            if (!tryAcquireCooldown(context.player())) {
                return WrenchResult.fail(MSG_COOLDOWN, Map.of("s", cooldownSecondsLabel(context.player())));
            }
            return WrenchResult.pass();
        }
    }

    private final class ConditionEvaluationStage implements WrenchPipelineStage {
        @Override
        public WrenchResult process(WrenchContext context) {
            if (context == null) {
                return WrenchResult.pass();
            }
            Optional<MultiblockInstance> instance = manager.getInstanceAt(context.clickedBlock().getLocation());
            if (conditionService.evaluate(context, instance.orElse(null))) {
                return WrenchResult.pass();
            }
            return WrenchResult.fail(MSG_INTERACTION_DENIED);
        }
    }

    private final class InteractionResolutionStage implements WrenchPipelineStage {
        @Override
        public WrenchResult process(WrenchContext context) {
            WrenchResult addonResult = dispatchAddonActions(context);
            if (!addonResult.isPass()) {
                resolvedInteractable = null;
                return addonResult;
            }
            resolvedInteractable = resolveBuiltInInteractable(context);
            return WrenchResult.pass();
        }
    }

    private final class ExecutionStage implements WrenchPipelineStage {
        @Override
        public WrenchResult process(WrenchContext context) {
            if (resolvedInteractable == null) {
                return WrenchResult.pass();
            }
            WrenchResult result = resolvedInteractable.onWrenchUse(context);
            resolvedInteractable = null;
            return result == null ? WrenchResult.pass() : result;
        }
    }

    private static final class WrenchConditionService {
        private final List<Map<String, String>> conditions;

        private WrenchConditionService() {
            this.conditions = List.of(Map.of("type", "player_permission", "permission", "mbe.disassemble"));
        }

        private boolean evaluate(WrenchContext context, MultiblockInstance instance) {
            if (!isDisassembleIntent(context, instance)) {
                return true;
            }
            for (Map<String, String> condition : conditions) {
                String type = condition.get("type");
                if ("player_permission".equalsIgnoreCase(type)) {
                    String permission = condition.get("permission");
                    if (permission == null || permission.isBlank()) {
                        continue;
                    }
                    Player player = context.player();
                    if (player == null || !player.hasPermission(permission)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isDisassembleIntent(WrenchContext context, MultiblockInstance instance) {
            if (context == null || context.action() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
                return false;
            }
            Block block = context.clickedBlock();
            if (block == null || instance == null || instance.anchorLocation() == null) {
                return false;
            }
            return block.getLocation().equals(instance.anchorLocation());
        }
    }

    private AssemblyReport tryAssemble(Block controller, WrenchContext context) {
        if (controller == null || context == null) {
            return null;
        }
        if (assembly != null) {
            AssemblyContext ctx = new AssemblyContext(
                    context.player(),
                    controller,
                    new dev.darkblade.mbe.api.service.interaction.InteractionIntent(
                            context.player(),
                            dev.darkblade.mbe.api.service.interaction.InteractionType.WRENCH_USE,
                            controller,
                            context.item(),
                            dev.darkblade.mbe.api.service.interaction.InteractionSource.WRENCH
                    )
            );
            return assembly.tryAssembleAt(controller, ctx);
        }

        Player player = context.player();
        for (MultiblockType type : manager.getTypes()) {
            if (type == null) {
                continue;
            }
            String trigger = type.assemblyTrigger();
            if (trigger != null && !trigger.isBlank() && !trigger.equalsIgnoreCase(AssemblyTriggerType.WRENCH_USE.id())) {
                continue;
            }
            if (!type.controllerMatcher().matches(controller)) {
                continue;
            }
            if (manager.getInstanceAt(controller.getLocation()).isPresent()) {
                continue;
            }
            Optional<MultiblockInstance> instance = manager.tryCreate(controller, type, player);
            if (instance.isPresent()) {
                return AssemblyReport.success(List.of(new AssemblyStepTrace(
                        "instance_create",
                        true,
                        "Instance created",
                        java.util.Map.of("multiblockId", type.id(), "trigger", AssemblyTriggerType.WRENCH_USE.id())
                )));
            }
        }
        return null;
    }

    private boolean isControllerBlock(Block block) {
        for (MultiblockType type : manager.getTypes()) {
            if (type != null && type.controllerMatcher().matches(block)) {
                return true;
            }
        }
        return false;
    }

    private void inspect(Player player, MultiblockInstance instance) {
        if (player == null) {
            return;
        }

        InspectionPipelineService pipeline = null;
        try {
            MultiBlockEngine plugin = MultiBlockEngine.getInstance();
            if (plugin != null && plugin.getAddonLifecycleService() != null) {
                pipeline = plugin.getAddonLifecycleService().getCoreService(InspectionPipelineService.class);
            }
        } catch (Throwable ignored) {
        }

        if (pipeline == null) {
            send(player, MSG_INSPECT_TITLE, Map.of());
            send(player, MSG_INSPECT_TYPE, Map.of("type", safe(instance.type().id())));
            send(player, MSG_INSPECT_STATE, Map.of("state", instance.state() == null ? "" : instance.state().name()));
            send(player, MSG_INSPECT_FACING, Map.of("facing", instance.facing() == null ? "" : instance.facing().name()));
            Location loc = instance.anchorLocation();
            String anchor = loc == null ? "" : (loc.getWorld() != null ? loc.getWorld().getName() : "") + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            send(player, MSG_INSPECT_ANCHOR, Map.of("anchor", anchor));

            List<String> components = describeRequiredComponents(instance.type());
            if (!components.isEmpty()) {
                send(player, MSG_INSPECT_COMPONENTS, Map.of());
                for (String line : components) {
                    send(player, CoreMessageKeys.WRENCH_INSPECT_COMPONENT_LINE, Map.of("line", line));
                }
            }
            return;
        }

        Inspectable inspectable = ctx -> {
            Map<String, InspectionEntry> out = new LinkedHashMap<>();

            out.put("type", new InspectionEntry("type", safe(instance.type().id()), EntryType.TEXT, InspectionLevel.PLAYER));
            out.put("state", new InspectionEntry("state", instance.state() == null ? "" : instance.state().name(), EntryType.TEXT, InspectionLevel.PLAYER));
            out.put("facing", new InspectionEntry("facing", instance.facing() == null ? "" : instance.facing().name(), EntryType.TEXT, InspectionLevel.PLAYER));

            Location loc = instance.anchorLocation();
            String anchor = loc == null ? "" : (loc.getWorld() != null ? loc.getWorld().getName() : "") + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            out.put("anchor", new InspectionEntry("anchor", anchor, EntryType.TEXT, InspectionLevel.PLAYER));

            List<String> components = describeRequiredComponents(instance.type());
            if (!components.isEmpty()) {
                out.put("components", new InspectionEntry("components", List.copyOf(components), EntryType.TEXT, InspectionLevel.PLAYER));
            }

            return new InspectionData(Map.copyOf(out));
        };

        InspectionRenderer renderer = (p, data, ctx) -> {
            send(p, MSG_INSPECT_TITLE, Map.of());

            InspectionEntry type = data.entries().get("type");
            if (type != null) {
                send(p, MSG_INSPECT_TYPE, Map.of("type", safe(String.valueOf(type.value()))));
            }

            InspectionEntry state = data.entries().get("state");
            if (state != null) {
                send(p, MSG_INSPECT_STATE, Map.of("state", safe(String.valueOf(state.value()))));
            }

            InspectionEntry facing = data.entries().get("facing");
            if (facing != null) {
                send(p, MSG_INSPECT_FACING, Map.of("facing", safe(String.valueOf(facing.value()))));
            }

            InspectionEntry anchor = data.entries().get("anchor");
            if (anchor != null) {
                send(p, MSG_INSPECT_ANCHOR, Map.of("anchor", safe(String.valueOf(anchor.value()))));
            }

            InspectionEntry comps = data.entries().get("components");
            if (comps != null && comps.value() instanceof List<?> list && !list.isEmpty()) {
                send(p, MSG_INSPECT_COMPONENTS, Map.of());
                for (Object line : list) {
                    if (line == null) continue;
                    send(p, CoreMessageKeys.WRENCH_INSPECT_COMPONENT_LINE, Map.of("line", String.valueOf(line)));
                }
            }
        };

        pipeline.inspect(player, InteractionSource.WRENCH, inspectable, renderer);
    }

    private List<String> describeRequiredComponents(MultiblockType type) {
        if (type == null) {
            return List.of();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PatternEntry entry : type.pattern()) {
            if (entry == null || entry.optional()) {
                continue;
            }
            String name = matcherToDisplay(entry.matcher());
            if (name.isBlank()) {
                continue;
            }
            counts.merge(name, 1, Integer::sum);
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            out.add(e.getKey() + " x" + e.getValue());
        }
        return out;
    }

    private String matcherToDisplay(BlockMatcher matcher) {
        if (matcher == null) {
            return "";
        }
        if (matcher instanceof ExactMaterialMatcher m) {
            return m.material() == null ? "" : m.material().name();
        }
        if (matcher instanceof TagMatcher m) {
            if (m.tag() == null || m.tag().getKey() == null) {
                return "";
            }
            return "#" + m.tag().getKey();
        }
        if (matcher instanceof AirMatcher) {
            return "AIR";
        }
        if (matcher instanceof BlockDataMatcher m) {
            return m.expectedData() == null ? "" : m.expectedData().getAsString();
        }
        if (matcher instanceof AnyOfMatcher m) {
            List<String> parts = new ArrayList<>();
            for (BlockMatcher sub : m.matchers()) {
                String s = matcherToDisplay(sub);
                if (!s.isBlank()) {
                    parts.add(s);
                }
            }
            return String.join(" | ", parts);
        }
        return matcher.getClass().getSimpleName();
    }

    private boolean isAnchorBlock(Block block, MultiblockInstance instance) {
        if (block == null || instance == null || instance.anchorLocation() == null) {
            return false;
        }
        return block.getLocation().equals(instance.anchorLocation());
    }

    private boolean isTillableSoil(Block block) {
        return block != null && TILLABLE_SOIL.contains(block.getType());
    }

    private boolean tryAcquireCooldown(Player player) {
        if (player == null) {
            return true;
        }
        Instant now = Instant.now();
        Instant prev = cooldowns.get(player.getUniqueId());
        if (prev != null && prev.plus(COOLDOWN).isAfter(now)) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), now);
        return true;
    }

    private String cooldownSecondsLabel(Player player) {
        Locale locale = Locale.ROOT;
        try {
            if (i18n != null && i18n.localeProvider() != null) {
                Locale resolved = i18n.localeProvider().localeOf(player);
                if (resolved != null) {
                    locale = resolved;
                }
            }
        } catch (Throwable ignored) {
        }
        return String.format(locale, "%.1f", COOLDOWN.toMillis() / 1000.0D);
    }

    private void playSuccess(Player player, Location loc) {
        if (player == null || loc == null) {
            return;
        }
        try {
            player.playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            player.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 1.0, 0.5), 12, 0.4, 0.4, 0.4, 0.0);
        } catch (Throwable ignored) {
        }
    }

    private void playFailure(Player player, Location loc) {
        if (player == null || loc == null) {
            return;
        }
        try {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            player.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0.5, 1.0, 0.5), 10, 0.3, 0.3, 0.3, 0.0);
        } catch (Throwable ignored) {
        }
    }

    private void playInspect(Player player, Location loc) {
        if (player == null || loc == null) {
            return;
        }
        try {
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        } catch (Throwable ignored) {
        }
    }

    private void playDisassemble(Player player, Location loc) {
        if (player == null || loc == null) {
            return;
        }
        try {
            player.playSound(loc, Sound.BLOCK_ANVIL_BREAK, 0.8f, 1.0f);
            player.spawnParticle(Particle.CLOUD, loc.clone().add(0.5, 1.0, 0.5), 14, 0.35, 0.35, 0.35, 0.0);
        } catch (Throwable ignored) {
        }
    }

    private String formatMissingForBestMatch(Block controller) {
        if (controller == null) {
            return "";
        }

        List<MultiblockType> candidates = new ArrayList<>();
        for (MultiblockType type : manager.getTypes()) {
            if (type == null) {
                continue;
            }
            String trigger = type.assemblyTrigger();
            if (trigger != null && !trigger.isBlank() && !trigger.equalsIgnoreCase(AssemblyTriggerType.WRENCH_USE.id())) {
                continue;
            }
            if (type.controllerMatcher().matches(controller)) {
                candidates.add(type);
            }
        }
        if (candidates.isEmpty()) {
            return "";
        }

        BlockFace preferred = BlockFace.NORTH;
        if (controller.getBlockData() instanceof Directional directional) {
            preferred = directional.getFacing();
        }

        MissingReport best = null;
        for (MultiblockType type : candidates) {
            for (BlockFace facing : candidateFacings(preferred)) {
                MissingReport r = computeMissing(controller, type, facing);
                if (r == null) {
                    continue;
                }
                if (best == null || r.missingCount < best.missingCount) {
                    best = r;
                }
                if (best != null && best.missingCount == 0) {
                    return "";
                }
            }
        }

        if (best == null || best.missing.isEmpty()) {
            return "";
        }

        int limit = 8;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(limit, best.missing.size()); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(best.missing.get(i));
        }
        if (best.missing.size() > limit) {
            sb.append(" (+").append(best.missing.size() - limit).append(")");
        }
        return sb.toString();
    }

    private List<BlockFace> candidateFacings(BlockFace preferred) {
        BlockFace p = preferred == null ? BlockFace.NORTH : preferred;
        List<BlockFace> out = new ArrayList<>(4);
        out.add(p);
        if (p != BlockFace.NORTH) out.add(BlockFace.NORTH);
        if (p != BlockFace.EAST) out.add(BlockFace.EAST);
        if (p != BlockFace.SOUTH) out.add(BlockFace.SOUTH);
        if (p != BlockFace.WEST) out.add(BlockFace.WEST);
        return out;
    }

    private MissingReport computeMissing(Block controller, MultiblockType type, BlockFace facing) {
        if (controller == null || type == null) {
            return null;
        }
        List<String> missing = new ArrayList<>();
        int missingCount = 0;

        for (PatternEntry entry : type.pattern()) {
            if (entry == null || entry.offset() == null) {
                continue;
            }
            Vector rotated = rotateVector(entry.offset(), facing);
            Block target = controller.getRelative(rotated.getBlockX(), rotated.getBlockY(), rotated.getBlockZ());
            if (!target.getChunk().isLoaded()) {
                if (!entry.optional()) {
                    missingCount++;
                    missing.add("chunk");
                }
                continue;
            }
            if (!entry.matcher().matches(target) && !entry.optional()) {
                missingCount++;
                String expected = matcherToDisplay(entry.matcher());
                missing.add("(" + rotated.getBlockX() + "," + rotated.getBlockY() + "," + rotated.getBlockZ() + ")=" + expected);
            }
        }

        return new MissingReport(missingCount, missing);
    }

    private Vector rotateVector(Vector v, BlockFace facing) {
        if (v == null) {
            return new Vector(0, 0, 0);
        }
        return switch (facing) {
            case NORTH -> v.clone();
            case EAST -> new Vector(-v.getZ(), v.getY(), v.getX());
            case SOUTH -> new Vector(-v.getX(), v.getY(), -v.getZ());
            case WEST -> new Vector(v.getZ(), v.getY(), -v.getX());
            default -> v.clone();
        };
    }

    private record MissingReport(int missingCount, List<String> missing) {
    }

    private boolean disassemble(Player player, MultiblockInstance instance) {
        MultiblockBreakEvent mbEvent = new MultiblockBreakEvent(instance, player);
        eventCaller.accept(mbEvent);
        if (mbEvent.isCancelled()) {
            return false;
        }

        for (Action a : instance.type().onBreakActions()) {
            executeActionSafely("BREAK", a, instance, null);
        }
        manager.destroyInstance(instance);
        return true;
    }

    private boolean isWrench(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemInstance instance;
        try {
            instance = itemStackBridge.fromItemStack(stack);
        } catch (Throwable t) {
            return false;
        }
        if (instance == null || instance.definition() == null) {
            return false;
        }
        ItemKey key = instance.definition().key();
        return WRENCH_KEY.equals(key);
    }

    private void send(Player player, String messageKey, Map<String, Object> params) {
        if (messageKey == null || messageKey.isBlank()) {
            return;
        }
        MessageKey key = resolveMessageKey(messageKey);
        if (key == null) {
            return;
        }
        send(player, key, params);
    }

    private MessageKey resolveMessageKey(String messageKey) {
        if (messageKey == null || messageKey.isBlank()) {
            return null;
        }
        int idx = messageKey.indexOf(':');
        if (idx > 0 && idx < messageKey.length() - 1) {
            return MessageKey.of(messageKey.substring(0, idx), messageKey.substring(idx + 1));
        }
        return MessageKey.of(ORIGIN, messageKey);
    }

    private void send(Player player, MessageKey key, Map<String, ?> params) {
        if (player == null || key == null) {
            return;
        }
        if (i18n == null) {
            return;
        }
        try {
            i18n.send(player, key, params);
        } catch (Throwable ignored) {
        }
    }

    private void executeActionSafely(String runtimePhase, Action action, MultiblockInstance instance, Player player) {
        try {
            if (player != null) {
                action.execute(instance, player);
            } else {
                action.execute(instance);
            }
        } catch (Throwable t) {
            String ownerId = action != null ? action.ownerId() : null;
            String typeKey = action != null ? action.typeKey() : null;

            String actionName = "unknown";
            if (typeKey != null && !typeKey.isBlank()) {
                int idx = typeKey.lastIndexOf(':');
                actionName = idx >= 0 ? typeKey.substring(idx + 1) : typeKey;
            } else if (action != null) {
                actionName = action.getClass().getSimpleName();
            }

            Object counter = instance != null ? instance.getVariable("counter") : null;
            String msg = "[" + runtimePhase + "] Action '" + actionName + "' failed Context: counter=" + counter + " Multiblock=" + (instance != null ? instance.type().id() : "unknown") + " Execution continued";

            MultiBlockEngine plugin = MultiBlockEngine.getInstance();
            if (plugin != null && plugin.getAddonLifecycleService() != null && ownerId != null && !ownerId.isBlank() && !"core".equalsIgnoreCase(ownerId)) {
                plugin.getAddonLifecycleService().failAddon(ownerId, AddonException.Phase.RUNTIME, msg, t, false);
                return;
            }

            CoreLogger core = plugin != null && plugin.getLoggingService() != null ? plugin.getLoggingService().core() : null;
            if (core != null) {
                core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.ERROR, msg, t, new LogKv[] {
                        LogKv.kv("phase", runtimePhase),
                        LogKv.kv("multiblock", instance != null ? instance.type().id() : "unknown"),
                        LogKv.kv("action", actionName)
                }, Set.of("wrench"));
            } else if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Runtime] " + msg + " Cause: " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
            }
        }
    }

    private void safeLogAddonActionError(Throwable t) {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        CoreLogger core = plugin != null && plugin.getLoggingService() != null ? plugin.getLoggingService().core() : null;
        if (core != null) {
            String msg = "Wrench action failed";
            core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.ERROR, msg, t, new LogKv[] {
                    LogKv.kv("where", "wrench.dispatch")
            }, Set.of("wrench"));
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
