package dev.darkblade.mbe.core.platform.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.wiring.PortBlockRef;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.api.wiring.PortDirection;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.core.application.service.item.DefaultItemService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.PdcItemStackBridge;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.core.domain.MultiblockSource;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerType;
import dev.darkblade.mbe.core.application.service.wrench.DefaultWrenchDispatcher;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MultiblockListenerInteractCancelTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void coreMultiblocksAreInstalledAndRegisteredOnEnable() {
        MultiBlockEngine plugin = MockBukkit.load(MultiBlockEngine.class);

        assertTrue(plugin.getManager().getType("mana_generator").isPresent());

        File multiblockDir = new File(plugin.getDataFolder(), "multiblocks");
        assertTrue(multiblockDir.exists());
        File defaultsDir = new File(multiblockDir, ".default");
        assertTrue(defaultsDir.exists());
        assertTrue(new File(defaultsDir, "mana_generator.yml").exists());
        assertTrue(new File(defaultsDir, "base_machine.yml").exists());
    }

    @Test
    void wrenchAssemblesCoreMultiblock() throws Exception {
        MultiBlockEngine plugin = MockBukkit.load(MultiBlockEngine.class);

        Location controllerLoc = new Location(world, 10, 64, 10);
        Block controller = world.getBlockAt(controllerLoc);
        controller.setType(Material.DIAMOND_BLOCK);

        world.getBlockAt(10, 63, 10).setType(Material.OBSIDIAN);
        world.getBlockAt(11, 63, 10).setType(Material.OBSIDIAN);
        world.getBlockAt(9, 63, 10).setType(Material.OBSIDIAN);
        world.getBlockAt(10, 63, 11).setType(Material.OBSIDIAN);
        world.getBlockAt(10, 63, 9).setType(Material.OBSIDIAN);
        world.getBlockAt(11, 63, 11).setType(Material.OBSIDIAN);
        world.getBlockAt(11, 63, 9).setType(Material.OBSIDIAN);
        world.getBlockAt(9, 63, 11).setType(Material.OBSIDIAN);
        world.getBlockAt(9, 63, 9).setType(Material.OBSIDIAN);

        world.getBlockAt(11, 64, 11).setType(Material.GOLD_BLOCK);
        world.getBlockAt(11, 64, 9).setType(Material.GOLD_BLOCK);
        world.getBlockAt(9, 64, 11).setType(Material.GOLD_BLOCK);
        world.getBlockAt(9, 64, 9).setType(Material.GOLD_BLOCK);

        ItemService itemService = plugin.getAddonLifecycleService().getCoreService(ItemService.class);
        ItemStackBridge itemStackBridge = plugin.getAddonLifecycleService().getCoreService(ItemStackBridge.class);
        assertNotNull(itemService);
        assertNotNull(itemStackBridge);

        ItemInstance wrenchInstance = itemService.factory().create(ItemKeys.of("mbe:wrench", 0));
        ItemStack wrench = itemStackBridge.toItemStack(wrenchInstance);

        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, wrench, controller, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(interactEvent);

        Optional<MultiblockInstance> created = plugin.getManager().getInstanceAt(controllerLoc);
        assertTrue(created.isPresent());
        assertEquals("mana_generator", created.get().type().id());
        assertTrue(interactEvent.isCancelled());
    }

    @Test
    void wrenchStillAssemblesAddonMultiblock() throws Exception {
        MultiBlockEngine plugin = MockBukkit.load(MultiBlockEngine.class);

        MultiblockType storageType = new MultiblockType(
                "storage:disk",
                "1.0",
                new Vector(0, 0, 0),
                b -> b != null && b.getType() == Material.EMERALD_BLOCK,
                List.of(new PatternEntry(new Vector(0, 0, 0), b -> b != null && b.getType() == Material.EMERALD_BLOCK, false)),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );
        plugin.getManager().registerType(storageType);

        Location controllerLoc = new Location(world, 15, 64, 15);
        Block controller = world.getBlockAt(controllerLoc);
        controller.setType(Material.EMERALD_BLOCK);

        ItemService itemService = plugin.getAddonLifecycleService().getCoreService(ItemService.class);
        ItemStackBridge itemStackBridge = plugin.getAddonLifecycleService().getCoreService(ItemStackBridge.class);
        assertNotNull(itemService);
        assertNotNull(itemStackBridge);

        ItemInstance wrenchInstance = itemService.factory().create(ItemKeys.of("mbe:wrench", 0));
        ItemStack wrench = itemStackBridge.toItemStack(wrenchInstance);

        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, wrench, controller, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(interactEvent);

        Optional<MultiblockInstance> created = plugin.getManager().getInstanceAt(controllerLoc);
        assertTrue(created.isPresent());
        assertEquals("storage:disk", created.get().type().id());
        assertTrue(interactEvent.isCancelled());
    }

    @Test
    void sneakRightClickAssemblyDoesNotCancelVanilla() throws Exception {
        MultiBlockEngine plugin = MockBukkit.load(MultiBlockEngine.class);

        MultiblockType type = new MultiblockType(
                "custom:sneak_form",
                "1.0",
                AssemblyTriggerType.SNEAK_RIGHT_CLICK.id(),
                new Vector(0, 0, 0),
                b -> b != null && b.getType() == Material.BEACON,
                List.of(new PatternEntry(new Vector(0, 0, 0), b -> b != null && b.getType() == Material.BEACON, false)),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20
        );
        plugin.getManager().registerType(type);

        Location controllerLoc = new Location(world, 20, 64, 20);
        Block controller = world.getBlockAt(controllerLoc);
        controller.setType(Material.BEACON);

        player.setSneaking(true);
        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, controller, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(interactEvent);

        Optional<MultiblockInstance> created = plugin.getManager().getInstanceAt(controllerLoc);
        assertTrue(created.isPresent());
        assertEquals("custom:sneak_form", created.get().type().id());
        assertFalse(interactEvent.isCancelled());
    }

    @Test
    void interactIsCancelledWhenMenuActionConsumesVanilla() throws Exception {
        Location loc = new Location(world, 10, 64, 10);

        Action openMenu = new Action() {
            @Override
            public void execute(MultiblockInstance instance, Player player) {
            }

            @Override
            public boolean shouldExecuteOnInteract(org.bukkit.event.block.Action interactAction) {
                return interactAction == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
            }

            @Override
            public boolean cancelsVanillaOnInteract(org.bukkit.event.block.Action interactAction) {
                return interactAction == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
            }
        };

        MultiblockInstance instance = new MultiblockInstance(dummyType("custom:menu_block", List.of(openMenu)), loc, BlockFace.NORTH);

        TestManager manager = new TestManager(instance);
        WrenchTestHarness harness = new WrenchTestHarness(manager);
        MultiblockListener listener = new MultiblockListener(manager, e -> {
        }, harness.dispatcher);

        Block block = world.getBlockAt(10, 64, 10);
        block.setType(Material.STONE);
        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, EquipmentSlot.HAND);

        listener.onInteract(interactEvent);

        assertTrue(interactEvent.isCancelled());
    }

    @Test
    void interactOnBeaconIsCancelledWhenMenuActionConsumesVanilla() throws Exception {
        Location loc = new Location(world, 10, 64, 10);

        Action openMenu = new Action() {
            @Override
            public void execute(MultiblockInstance instance, Player player) {
            }

            @Override
            public boolean shouldExecuteOnInteract(org.bukkit.event.block.Action interactAction) {
                return interactAction == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
            }

            @Override
            public boolean cancelsVanillaOnInteract(org.bukkit.event.block.Action interactAction) {
                return interactAction == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
            }
        };

        MultiblockInstance instance = new MultiblockInstance(dummyType("custom:beacon_menu_block", List.of(openMenu)), loc, BlockFace.NORTH);

        TestManager manager = new TestManager(instance);
        WrenchTestHarness harness = new WrenchTestHarness(manager);
        MultiblockListener listener = new MultiblockListener(manager, e -> {
        }, harness.dispatcher);

        Block beacon = world.getBlockAt(10, 64, 10);
        beacon.setType(Material.BEACON);
        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, beacon, EquipmentSlot.HAND);

        listener.onInteract(interactEvent);

        assertTrue(interactEvent.isCancelled());
    }

    @Test
    void interactOnControllerWithWrenchIsCancelledEvenWhenCreationIsBlocked() throws Exception {
        Location loc = new Location(world, 10, 64, 10);

        MultiblockType type = new MultiblockType(
                "custom:blocked_form",
                "1.0",
                new Vector(0, 0, 0),
                b -> b != null && b.getType() == Material.BEACON,
                List.of(new PatternEntry(new Vector(0, 0, 0), b -> b != null && b.getType() == Material.BEACON, false)),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );

        Block beacon = world.getBlockAt(10, 64, 10);
        beacon.setType(Material.BEACON);

        ControllerNoCreateManager manager = new ControllerNoCreateManager(type);
        WrenchTestHarness harness = new WrenchTestHarness(manager);
        MultiblockListener listener = new MultiblockListener(manager, e -> {
        }, harness.dispatcher);

        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, harness.wrench, beacon, EquipmentSlot.HAND);

        listener.onInteract(interactEvent);

        assertTrue(interactEvent.isCancelled());
    }

    @Test
    void portResolutionServiceResolvesOffsetsUsingInstanceFacing() {
        MultiBlockEngine plugin = MockBukkit.load(MultiBlockEngine.class);

        PortResolutionService svc = plugin.getAddonLifecycleService().getCoreService(PortResolutionService.class);
        assertNotNull(svc);

        Map<String, PortDefinition> ports = Map.of(
                "p",
                new PortDefinition("p", PortDirection.INPUT, "energy", new PortBlockRef.Offset(1, 0, 0), Set.of("accept"))
        );

        MultiblockType type = new MultiblockType(
                "test:ports",
                "1.0",
                AssemblyTriggerType.WRENCH_USE.id(),
                new Vector(0, 0, 0),
                block -> true,
                List.of(),
                false,
                Map.of(),
                Map.of(),
                ports,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20
        );

        Location controller = new Location(world, 0, 64, 0);
        MultiblockInstance north = new MultiblockInstance(type, controller, BlockFace.NORTH);
        MultiblockInstance east = new MultiblockInstance(type, controller, BlockFace.EAST);
        MultiblockInstance south = new MultiblockInstance(type, controller, BlockFace.SOUTH);
        MultiblockInstance west = new MultiblockInstance(type, controller, BlockFace.WEST);

        assertEquals(new Location(world, 1, 64, 0), svc.resolvePort(north, "p").orElseThrow());
        assertEquals(new Location(world, 0, 64, 1), svc.resolvePort(east, "p").orElseThrow());
        assertEquals(new Location(world, -1, 64, 0), svc.resolvePort(south, "p").orElseThrow());
        assertEquals(new Location(world, 0, 64, -1), svc.resolvePort(west, "p").orElseThrow());

        assertEquals(controller, svc.resolveBlock(north, new PortBlockRef.Controller()).orElseThrow());
    }

    @Test
    void reloadDoesNotDropRuntimeTypes() {
        MultiBlockEngine plugin = MockBukkit.load(MultiBlockEngine.class);

        MultiblockType runtimeType = new MultiblockType(
                "addon:test_runtime",
                "1.0",
                new Vector(0, 0, 0),
                block -> true,
                List.of(),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );

        plugin.getManager().registerType(runtimeType, new MultiblockSource(MultiblockSource.Type.USER_DEFINED, "<runtime>"));
        assertTrue(plugin.getManager().getType("addon:test_runtime").isPresent());

        plugin.getManager().reloadTypesWithSources(List.of(), Map.of());

        assertTrue(plugin.getManager().getType("addon:test_runtime").isPresent());
    }

    @Test
    void interactOnControllerWithoutWrenchIsNotCancelledWhenNoInstance() throws Exception {
        MultiblockType type = new MultiblockType(
                "custom:controller_only",
                "1.0",
                new Vector(0, 0, 0),
                b -> b != null && b.getType() == Material.BEACON,
                List.of(new PatternEntry(new Vector(0, 0, 0), b -> b != null && b.getType() == Material.BEACON, false)),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );

        MultiblockRuntimeService manager = new MultiblockRuntimeService();
        manager.registerType(type);
        WrenchTestHarness harness = new WrenchTestHarness(manager);
        MultiblockListener listener = new MultiblockListener(manager, e -> {
        }, harness.dispatcher);

        Block beacon = world.getBlockAt(10, 64, 10);
        beacon.setType(Material.BEACON);
        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, beacon, EquipmentSlot.HAND);

        listener.onInteract(interactEvent);

        assertFalse(interactEvent.isCancelled());
    }

    private static PlayerInteractEvent newPlayerInteractEvent(Player player, org.bukkit.event.block.Action action, ItemStack item, Block clickedBlock, EquipmentSlot hand) throws Exception {
        for (Constructor<?> c : PlayerInteractEvent.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 6
                    && p[0] == Player.class
                    && p[1] == org.bukkit.event.block.Action.class
                    && p[2].getName().equals("org.bukkit.inventory.ItemStack")
                    && p[3] == Block.class
                    && p[4] == BlockFace.class
                    && p[5] == EquipmentSlot.class) {
                return (PlayerInteractEvent) c.newInstance(player, action, item, clickedBlock, BlockFace.UP, hand);
            }
            if (p.length == 5
                    && p[0] == Player.class
                    && p[1] == org.bukkit.event.block.Action.class
                    && p[2].getName().equals("org.bukkit.inventory.ItemStack")
                    && p[3] == Block.class
                    && p[4] == BlockFace.class) {
                return (PlayerInteractEvent) c.newInstance(player, action, item, clickedBlock, BlockFace.UP);
            }
        }
        throw new IllegalStateException("No compatible PlayerInteractEvent constructor found");
    }

    private static final class WrenchTestHarness {
        private static final ItemKey WRENCH_KEY = ItemKeys.of("mbe:wrench", 0);

        private final DefaultItemService items;
        private final PdcItemStackBridge bridge;
        private final ItemStack wrench;
        private final WrenchDispatcher dispatcher;

        private WrenchTestHarness(MultiblockRuntimeService manager) {
            this.items = new DefaultItemService();
            this.bridge = new PdcItemStackBridge(items);
            this.items.registry().register(new ItemDefinition() {
                @Override
                public ItemKey key() {
                    return WRENCH_KEY;
                }

                @Override
                public String displayName() {
                    return "Wrench";
                }

                @Override
                public Map<String, Object> properties() {
                    return Map.of("material", "IRON_HOE");
                }
            });
            ItemInstance instance = items.factory().create(WRENCH_KEY);
            this.wrench = bridge.toItemStack(instance);
            this.dispatcher = new DefaultWrenchDispatcher(manager, bridge, null, e -> {
            });
        }
    }

    private static MultiblockType dummyType(String id, List<Action> onInteract) {
        return new MultiblockType(
                id,
                "1.0",
                new Vector(0, 0, 0),
                block -> false,
                List.of(),
                false,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                onInteract,
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );
    }

    private static final class TestManager extends MultiblockRuntimeService {
        private final MultiblockInstance instance;

        private TestManager(MultiblockInstance instance) {
            this.instance = instance;
        }

        @Override
        public Optional<MultiblockInstance> getInstanceAt(Location loc) {
            return Optional.of(instance);
        }
    }

    private static final class ControllerNoCreateManager extends MultiblockRuntimeService {
        private ControllerNoCreateManager(MultiblockType type) {
            registerType(type);
        }

        @Override
        public Optional<MultiblockInstance> tryCreate(Block anchor, MultiblockType type, Player player) {
            return Optional.empty();
        }
    }
}
