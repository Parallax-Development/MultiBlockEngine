package dev.darkblade.mbe.core.infrastructure.config.block;

import dev.darkblade.mbe.api.block.BlockBuilder;
import dev.darkblade.mbe.api.block.BlockDefinition;
import dev.darkblade.mbe.api.block.BlockKey;
import dev.darkblade.mbe.api.block.BlockRegistry;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class BuiltinBlockLoader {
    private final BlockRegistry registry;
    private final CoreLogger log;

    public BuiltinBlockLoader(BlockRegistry registry, CoreLogger log) {
        this.registry = registry;
        this.log = log;
    }

    public void loadFromDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                String id = config.getString("id");
                if (id == null) {
                    id = file.getName().replace(".yml", "");
                }

                BlockKey key;
                if (!id.contains(":")) {
                    key = BlockKey.of("mbe:" + id);
                } else {
                    key = BlockKey.of(id);
                }

                BlockBuilder builder = new BlockBuilder(key);

                if (config.contains("display_name")) {
                    builder.displayName(new DisplayNameConfig("raw", false, config.getString("display_name")));
                }

                if (config.contains("material")) {
                    builder.blockMaterial(config.getString("material"));
                }

                if (config.contains("assembly_trigger")) {
                    builder.assemblyTrigger(config.getString("assembly_trigger"));
                }

                if (config.contains("on_interact")) {
                    for (Object obj : config.getList("on_interact")) {
                        if (obj instanceof java.util.Map<?, ?> map) {
                            String action = (String) map.get("action");
                            if ("open_panel".equals(action)) {
                                String panelId = (String) map.get("panel");
                                builder.onInteract(new dev.darkblade.mbe.core.domain.action.OpenPanelAction(panelId));
                            }
                        }
                    }
                }

                BlockDefinition def = builder.build();
                registry.register(def);

            } catch (Exception e) {
                log.error("Failed to load built-in block", e, dev.darkblade.mbe.api.logging.LogKv.kv("file", file.getName()));
            }
        }
    }
}
