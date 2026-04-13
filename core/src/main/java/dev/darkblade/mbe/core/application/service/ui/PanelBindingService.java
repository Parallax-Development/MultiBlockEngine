package dev.darkblade.mbe.core.application.service.ui;

import dev.darkblade.mbe.api.ui.binding.PanelBinding;
import dev.darkblade.mbe.api.ui.binding.PanelBindingLinkService;
import dev.darkblade.mbe.api.ui.binding.PanelBindingMutationService;
import dev.darkblade.mbe.api.ui.binding.PanelBindingRegistry;
import dev.darkblade.mbe.core.application.service.ManagedCoreService;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PanelBindingService implements PanelBindingRegistry, PanelBindingMutationService, PanelBindingLinkService, ManagedCoreService {
    private final File file;
    private final Map<UUID, PanelBinding> bindingsById = new ConcurrentHashMap<>();
    private final Map<BlockPosition, PanelBinding> bindingsByPosition = new ConcurrentHashMap<>();
    private final Map<String, BindingTrigger> triggers = new ConcurrentHashMap<>();

    public PanelBindingService(File file, InteractionRouter router) {
        this.file = Objects.requireNonNull(file, "file");
        registerTrigger(new ClickBindingTrigger(Objects.requireNonNull(router, "router")));
    }

    public void registerTrigger(BindingTrigger trigger) {
        if (trigger == null || trigger.id() == null || trigger.id().isBlank()) {
            return;
        }
        triggers.put(trigger.id().trim().toLowerCase(Locale.ROOT), trigger);
    }

    public synchronized Optional<PanelBinding> createBinding(String panelId, Block block, String triggerType) {
        Objects.requireNonNull(panelId, "panelId");
        Objects.requireNonNull(triggerType, "triggerType");
        BlockPosition position = BlockPosition.fromBlock(block).orElse(null);
        if (position == null) {
            return Optional.empty();
        }
        if (bindingsByPosition.containsKey(position)) {
            return Optional.empty();
        }
        BindingTrigger trigger = resolveTrigger(triggerType).orElse(null);
        if (trigger == null) {
            throw new IllegalArgumentException("triggerType");
        }
        PanelBinding binding = new PanelBinding(
                UUID.randomUUID(),
                panelId,
                position.world(),
                position.x(),
                position.y(),
                position.z(),
                trigger.id()
        );
        bindingsById.put(binding.id(), binding);
        bindingsByPosition.put(position, binding);
        trigger.register(binding);
        save();
        return Optional.of(binding);
    }

    @Override
    public synchronized boolean linkPanelToBlock(String panelId, Block block, String triggerType) {
        return createBinding(panelId, block, triggerType).isPresent();
    }

    public synchronized boolean removeBinding(UUID bindingId) {
        if (bindingId == null) {
            return false;
        }
        PanelBinding removed = bindingsById.remove(bindingId);
        if (removed == null) {
            return false;
        }
        bindingsByPosition.remove(new BlockPosition(removed.world(), removed.x(), removed.y(), removed.z()));
        resolveTrigger(removed.triggerType()).ifPresent(trigger -> trigger.unregister(removed));
        save();
        return true;
    }

    @Override
    public synchronized boolean unlinkByBlock(Block block) {
        PanelBinding binding = getByBlock(block).orElse(null);
        if (binding == null) {
            return false;
        }
        return removeBinding(binding.id());
    }

    @Override
    public synchronized int unlinkByWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return 0;
        }
        String worldKey = worldName.trim();
        List<UUID> toRemove = new ArrayList<>();
        for (PanelBinding binding : bindingsById.values()) {
            if (binding.world().equalsIgnoreCase(worldKey)) {
                toRemove.add(binding.id());
            }
        }
        int removed = 0;
        for (UUID id : toRemove) {
            if (removeBinding(id)) {
                removed++;
            }
        }
        return removed;
    }

    public synchronized void load() {
        clearInMemory();
        if (!file.exists()) {
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rows = yaml.getMapList("bindings");
        for (Map<?, ?> row : rows) {
            PanelBinding binding = parse(row).orElse(null);
            if (binding == null) {
                continue;
            }
            BlockPosition position = new BlockPosition(binding.world(), binding.x(), binding.y(), binding.z());
            if (bindingsByPosition.containsKey(position)) {
                continue;
            }
            BindingTrigger trigger = resolveTrigger(binding.triggerType()).orElse(null);
            if (trigger == null) {
                continue;
            }
            bindingsById.put(binding.id(), binding);
            bindingsByPosition.put(position, binding);
            trigger.register(binding);
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PanelBinding binding : bindingsById.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", binding.id().toString());
            row.put("panel", binding.panelId());
            row.put("world", binding.world());
            row.put("x", binding.x());
            row.put("y", binding.y());
            row.put("z", binding.z());
            row.put("trigger", binding.triggerType());
            rows.add(row);
        }
        yaml.set("bindings", rows);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            yaml.save(file);
        } catch (IOException ignored) {
        }
    }

    public Optional<BindingTrigger> resolveTrigger(String triggerType) {
        if (triggerType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(triggers.get(triggerType.trim().toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<PanelBinding> getByBlock(Block block) {
        return BlockPosition.fromBlock(block).map(bindingsByPosition::get).filter(Objects::nonNull);
    }

    @Override
    public Collection<PanelBinding> all() {
        return List.copyOf(bindingsById.values());
    }

    @Override
    public String getManagedCoreServiceId() {
        return "mbe:panel-bindings";
    }

    @Override
    public void onCoreLoad() {
        load();
    }

    @Override
    public void onCoreDisable() {
        save();
    }

    private void clearInMemory() {
        for (PanelBinding binding : bindingsById.values()) {
            resolveTrigger(binding.triggerType()).ifPresent(trigger -> trigger.unregister(binding));
        }
        bindingsById.clear();
        bindingsByPosition.clear();
    }

    private Optional<PanelBinding> parse(Map<?, ?> row) {
        if (row == null || row.isEmpty()) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(String.valueOf(row.get("id")));
            String panel = String.valueOf(row.get("panel"));
            String world = String.valueOf(row.get("world"));
            int x = asInt(row.get("x"));
            int y = asInt(row.get("y"));
            int z = asInt(row.get("z"));
            String trigger = String.valueOf(row.get("trigger"));
            return Optional.of(new PanelBinding(id, panel, world, x, y, z, trigger));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private int asInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw));
    }
}
