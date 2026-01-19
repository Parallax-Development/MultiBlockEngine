package com.darkbladedev.engine.model.matcher;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import com.darkbladedev.engine.model.PatternBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class SemanticBlockDataMatcherTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void ignoresDynamicWaterloggedWhenNotRequired() {
        PatternBlock pattern = new PatternBlock(
            Material.STONE_BRICK_STAIRS,
            Map.of(
                "facing", "north",
                "half", "bottom"
            )
        );
        BlockDataMatcher matcher = new BlockDataMatcher(pattern);

        Block b = world.getBlockAt(0, 64, 0);
        BlockData data = Bukkit.createBlockData("minecraft:stone_brick_stairs[facing=north,half=bottom,waterlogged=true]");
        b.setBlockData(data);

        assertTrue(matcher.match(b).success());
    }

    @Test
    void failsWhenExplicitPropertyMismatches() {
        PatternBlock pattern = new PatternBlock(
            Material.STONE_BRICK_STAIRS,
            Map.of(
                "facing", "north",
                "half", "bottom"
            )
        );
        BlockDataMatcher matcher = new BlockDataMatcher(pattern);

        Block b = world.getBlockAt(0, 64, 0);
        BlockData data = Bukkit.createBlockData("minecraft:stone_brick_stairs[facing=east,half=bottom,waterlogged=true]");
        b.setBlockData(data);

        var res = matcher.match(b);
        assertFalse(res.success());
        assertTrue(res.reason().contains("facing"));
    }
}

