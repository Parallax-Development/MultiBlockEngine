package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.tool.ToolItem;
import dev.darkblade.mbe.api.tool.event.ToolModeFailEvent;
import dev.darkblade.mbe.api.tool.event.ToolModePostExecuteEvent;
import dev.darkblade.mbe.api.tool.event.ToolModePreExecuteEvent;
import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.api.tool.mode.ToolModeRegistry;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class ToolModeExecutionService implements MBEService {

    public static final ItemKey WRENCH_KEY = ItemKeys.of("mbe:wrench", 0);
    private static final String WRENCH_TOOL_ID = "wrench";
    private static final String WIRE_CUTTER_TOOL_ID = "wire_cutter";
    private static final NamespacedKey TOOL_ID_KEY = Objects.requireNonNull(NamespacedKey.fromString("mbe:tool_id"));
    private static final NamespacedKey TOOL_MODE_KEY = Objects.requireNonNull(NamespacedKey.fromString("mbe:tool_mode"));

    private final ItemStackBridge itemStackBridge;
    private final ToolModeRegistry modeRegistry;
    private final ToolSessionService sessionService;
    private final ToolModeMetricsService metricsService;
    private final Consumer<Event> eventCaller;
    private final Map<String, ToolItem> toolItems = new LinkedHashMap<>();

    public ToolModeExecutionService(
            ItemStackBridge itemStackBridge,
            ToolModeRegistry modeRegistry,
            ToolSessionService sessionService,
            ToolModeMetricsService metricsService,
            Consumer<Event> eventCaller
    ) {
        this.itemStackBridge = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
        this.modeRegistry = Objects.requireNonNull(modeRegistry, "modeRegistry");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService");
        this.eventCaller = Objects.requireNonNull(eventCaller, "eventCaller");
    }

    @Override
    public String getServiceId() {
        return "mbe:tool.execution";
    }

    public void registerToolItem(ToolItem toolItem) {
        Objects.requireNonNull(toolItem, "toolItem");
        String toolId = normalize(toolItem.getId());
        if (toolId.isBlank()) {
            throw new IllegalArgumentException("toolItem.id");
        }
        toolItems.put(toolId, toolItem);
    }

    public Optional<String> resolveToolId(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String pdcTool = meta.getPersistentDataContainer().get(TOOL_ID_KEY, PersistentDataType.STRING);
            String normalized = normalize(pdcTool);
            if (!normalized.isBlank()) {
                return Optional.of(normalized);
            }
        }
        ItemInstance instance;
        try {
            instance = itemStackBridge.fromItemStack(stack);
        } catch (Throwable t) {
            return Optional.empty();
        }
        if (instance == null || instance.definition() == null || instance.definition().key() == null || instance.definition().key().id() == null) {
            return Optional.empty();
        }
        ItemKey key = instance.definition().key();
        if (WRENCH_KEY.equals(key)) {
            writeToolPdc(stack, WRENCH_TOOL_ID, null);
            return Optional.of(WRENCH_TOOL_ID);
        }
        String inferred = normalize(key.id().key());
        if (WIRE_CUTTER_TOOL_ID.equals(inferred)) {
            writeToolPdc(stack, WIRE_CUTTER_TOOL_ID, null);
            return Optional.of(WIRE_CUTTER_TOOL_ID);
        }
        return Optional.empty();
    }

    public WrenchResult dispatch(WrenchContext context) {
        if (context == null || context.item() == null) {
            return WrenchResult.pass();
        }
        Optional<String> toolIdOpt = resolveToolId(context.item());
        if (toolIdOpt.isEmpty()) {
            return WrenchResult.pass();
        }
        String toolId = toolIdOpt.get();
        ToolItem tool = toolItems.get(normalize(toolId));
        if (tool == null || tool.getSupportedModes() == null || tool.getSupportedModes().isEmpty()) {
            return WrenchResult.pass();
        }
        String modeId = modeIdFromPdc(context.item(), tool).orElse(normalize(tool.getSupportedModes().getFirst()));
        if (modeId.isBlank()) {
            return WrenchResult.pass();
        }
        if (shouldCycleMode(context, tool)) {
            String cycled = cycleMode(context.item(), tool, modeId);
            if (!cycled.isBlank() && context.player() != null) {
                sessionService.clear(context.player().getUniqueId(), modeId);
            }
            return cycled.isBlank() ? WrenchResult.pass() : WrenchResult.success(null);
        }
        Optional<ToolMode> modeOpt = modeRegistry.get(modeId);
        if (modeOpt.isEmpty()) {
            return WrenchResult.pass();
        }
        ToolMode mode = modeOpt.get();
        if (!toolSupportsMode(tool, mode.getId()) || !mode.supports(context)) {
            return WrenchResult.pass();
        }
        ToolModePreExecuteEvent preEvent = new ToolModePreExecuteEvent(toolId, modeId, context);
        eventCaller.accept(preEvent);
        if (preEvent.isCancelled()) {
            metricsService.recordFailure(modeId);
            eventCaller.accept(new ToolModeFailEvent(toolId, modeId, context, "cancelled"));
            return WrenchResult.fail("core.wrench.interaction.denied");
        }
        writeToolPdc(context.item(), tool.getId(), mode.getId());
        metricsService.recordAttempt(modeId);
        try {
            WrenchResult result = mode.handle(context);
            WrenchResult safe = result == null ? WrenchResult.pass() : result;
            if (safe.isFail()) {
                metricsService.recordFailure(modeId);
                eventCaller.accept(new ToolModeFailEvent(toolId, modeId, context, safe.messageKey() == null ? "mode_failed" : safe.messageKey()));
            } else if (safe.isSuccess()) {
                metricsService.recordSuccess(modeId);
            }
            eventCaller.accept(new ToolModePostExecuteEvent(toolId, modeId, context, safe));
            return safe;
        } catch (Throwable t) {
            metricsService.recordFailure(modeId);
            eventCaller.accept(new ToolModeFailEvent(toolId, modeId, context, t.getClass().getSimpleName()));
            return WrenchResult.fail("core.wrench.interaction.denied");
        }
    }

    public static boolean isWrenchToolId(String toolId) {
        return WRENCH_TOOL_ID.equals(normalize(toolId));
    }

    private static boolean shouldCycleMode(WrenchContext context, ToolItem toolItem) {
        return context.player() != null
                && context.player().isSneaking()
                && context.action() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                && toolItem != null
                && toolItem.getSupportedModes() != null
                && toolItem.getSupportedModes().size() > 1;
    }

    private String cycleMode(ItemStack stack, ToolItem tool, String currentMode) {
        List<String> supported = tool.getSupportedModes();
        if (supported == null || supported.isEmpty()) {
            return "";
        }
        int currentIndex = 0;
        String normalizedCurrent = normalize(currentMode);
        for (int i = 0; i < supported.size(); i++) {
            if (normalize(supported.get(i)).equals(normalizedCurrent)) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % supported.size();
        String next = normalize(supported.get(nextIndex));
        if (next.isBlank()) {
            return "";
        }
        writeToolPdc(stack, tool.getId(), next);
        return next;
    }

    private Optional<String> modeIdFromPdc(ItemStack stack, ToolItem tool) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String mode = pdc.get(TOOL_MODE_KEY, PersistentDataType.STRING);
            String normalized = normalize(mode);
            if (!normalized.isBlank() && toolSupportsMode(tool, normalized)) {
                return Optional.of(normalized);
            }
        }
        if (tool.getSupportedModes() == null || tool.getSupportedModes().isEmpty()) {
            return Optional.empty();
        }
        String fallback = normalize(tool.getSupportedModes().getFirst());
        if (fallback.isBlank()) {
            return Optional.empty();
        }
        writeToolPdc(stack, tool.getId(), fallback);
        return Optional.of(fallback);
    }

    private static boolean toolSupportsMode(ToolItem tool, String modeId) {
        if (tool == null || modeId == null || modeId.isBlank() || tool.getSupportedModes() == null) {
            return false;
        }
        String normalized = normalize(modeId);
        for (String value : tool.getSupportedModes()) {
            if (normalize(value).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void writeToolPdc(ItemStack stack, String toolId, String modeId) {
        if (stack == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String normalizedTool = normalize(toolId);
        if (!normalizedTool.isBlank()) {
            pdc.set(TOOL_ID_KEY, PersistentDataType.STRING, normalizedTool);
        }
        String normalizedMode = normalize(modeId);
        if (!normalizedMode.isBlank()) {
            pdc.set(TOOL_MODE_KEY, PersistentDataType.STRING, normalizedMode);
        }
        stack.setItemMeta(meta);
    }
}
