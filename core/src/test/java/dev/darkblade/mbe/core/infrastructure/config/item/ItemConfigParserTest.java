package dev.darkblade.mbe.core.infrastructure.config.item;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.core.application.service.item.DefaultItemService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.PdcItemStackBridge;
import dev.darkblade.mbe.core.infrastructure.config.parser.item.ItemConfigParser;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemConfigParserTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void parseValidItemsFile() throws Exception {
        Path temp = Files.createTempFile("items-", ".yml");
        Files.writeString(temp, """
                items:
                  wrench:
                    material: IRON_HOE
                    display-name: "mbe:core.items.wrench.display_name"
                    lore:
                      - "mbe:core.items.wrench.lore.assemble"
                    custom-model-data: 9
                    flags:
                      - HIDE_ATTRIBUTES
                  blueprint:
                    key: mbe:blueprint
                    material: PAPER
                    unstackable: true
                """);

        ItemConfigParser parser = new ItemConfigParser(null);
        Map<ItemKey, YamlItemDefinition> parsed = parser.parse(temp.toFile());

        assertEquals(2, parsed.size());
        assertTrue(parsed.containsKey(ItemKeys.of("mbe:wrench", 0)));
        assertTrue(parsed.containsKey(ItemKeys.of("mbe:blueprint", 0)));
        YamlItemDefinition wrench = parsed.get(ItemKeys.of("mbe:wrench", 0));
        assertNotNull(wrench);
        assertEquals("IRON_HOE", wrench.properties().get("material"));
        assertEquals(9, wrench.properties().get("custom-model-data"));
    }

    @Test
    void parseWithInvalidEntriesKeepsValidOnes() throws Exception {
        Path temp = Files.createTempFile("items-invalid-", ".yml");
        Files.writeString(temp, """
                items:
                  broken:
                    material: INVALID_MATERIAL
                  valid:
                    material: PAPER
                """);

        ItemConfigParser parser = new ItemConfigParser(null);
        Map<ItemKey, YamlItemDefinition> parsed = parser.parse(temp.toFile());

        assertEquals(1, parsed.size());
        assertTrue(parsed.containsKey(ItemKeys.of("mbe:valid", 0)));
    }

    @Test
    void bridgeBuildsItemStackFromYamlDefinition() throws Exception {
        Path temp = Files.createTempFile("items-stack-", ".yml");
        Files.writeString(temp, """
                items:
                  wrench:
                    material: IRON_HOE
                    display-name: "Wrench"
                    lore:
                      - "Line 1"
                    custom-model-data: 3
                    flags:
                      - HIDE_ATTRIBUTES
                """);

        ItemConfigParser parser = new ItemConfigParser(null);
        Map<ItemKey, YamlItemDefinition> parsed = parser.parse(temp.toFile());

        DefaultItemService itemService = new DefaultItemService();
        for (ItemDefinition definition : parsed.values()) {
            itemService.registry().register(definition);
        }

        PdcItemStackBridge bridge = new PdcItemStackBridge(itemService);
        ItemStack stack = bridge.toItemStack(itemService.factory().create(ItemKeys.of("mbe:wrench", 0)));

        assertEquals(Material.IRON_HOE, stack.getType());
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertEquals(3, meta.getCustomModelData());
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
        assertTrue(meta.lore() != null && !meta.lore().isEmpty());
    }
}
