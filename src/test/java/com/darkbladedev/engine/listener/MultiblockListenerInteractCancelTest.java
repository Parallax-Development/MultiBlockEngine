package com.darkbladedev.engine.listener;

import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.DisplayNameConfig;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.action.Action;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MultiblockListenerInteractCancelTest {

    @Test
    void interactIsCancelledWhenMenuActionConsumesVanilla() throws Exception {
        UUID worldId = UUID.randomUUID();
        World world = worldProxy(worldId);
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
        MultiblockListener listener = new MultiblockListener(manager, e -> {
        });

        Block block = blockProxy(loc, Material.STONE);
        Player player = playerProxy();
        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, block, EquipmentSlot.HAND);

        listener.onInteract(interactEvent);

        assertTrue(interactEvent.isCancelled());
    }

    @Test
    void interactOnBeaconIsCancelledWhenMenuActionConsumesVanilla() throws Exception {
        UUID worldId = UUID.randomUUID();
        World world = worldProxy(worldId);
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
        MultiblockListener listener = new MultiblockListener(manager, e -> {
        });

        Block beacon = blockProxy(loc, Material.BEACON);
        Player player = playerProxy();
        PlayerInteractEvent interactEvent = newPlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, beacon, EquipmentSlot.HAND);

        listener.onInteract(interactEvent);

        assertTrue(interactEvent.isCancelled());
    }

    private static PlayerInteractEvent newPlayerInteractEvent(Player player, org.bukkit.event.block.Action action, Block clickedBlock, EquipmentSlot hand) throws Exception {
        for (Constructor<?> c : PlayerInteractEvent.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 6
                    && p[0] == Player.class
                    && p[1] == org.bukkit.event.block.Action.class
                    && p[2].getName().equals("org.bukkit.inventory.ItemStack")
                    && p[3] == Block.class
                    && p[4] == BlockFace.class
                    && p[5] == EquipmentSlot.class) {
                return (PlayerInteractEvent) c.newInstance(player, action, null, clickedBlock, BlockFace.UP, hand);
            }
            if (p.length == 5
                    && p[0] == Player.class
                    && p[1] == org.bukkit.event.block.Action.class
                    && p[2].getName().equals("org.bukkit.inventory.ItemStack")
                    && p[3] == Block.class
                    && p[4] == BlockFace.class) {
                return (PlayerInteractEvent) c.newInstance(player, action, null, clickedBlock, BlockFace.UP);
            }
        }
        throw new IllegalStateException("No compatible PlayerInteractEvent constructor found");
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

    private static World worldProxy(UUID uid) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUID") && method.getParameterCount() == 0) {
                        return uid;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == long.class) return 0L;
                        if (method.getReturnType() == float.class) return 0f;
                        if (method.getReturnType() == double.class) return 0d;
                        if (method.getReturnType() == short.class) return (short) 0;
                        if (method.getReturnType() == byte.class) return (byte) 0;
                        if (method.getReturnType() == char.class) return (char) 0;
                    }
                    return null;
                }
        );
    }

    private static Block blockProxy(Location location, Material type) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getLocation") && method.getParameterCount() == 0) {
                        return location;
                    }
                    if (method.getName().equals("getType") && method.getParameterCount() == 0) {
                        return type;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == long.class) return 0L;
                        if (method.getReturnType() == float.class) return 0f;
                        if (method.getReturnType() == double.class) return 0d;
                        if (method.getReturnType() == short.class) return (short) 0;
                        if (method.getReturnType() == byte.class) return (byte) 0;
                        if (method.getReturnType() == char.class) return (char) 0;
                    }
                    return null;
                }
        );
    }

    private static Player playerProxy() {
        UUID uuid = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId") && method.getParameterCount() == 0) {
                        return uuid;
                    }
                    if (method.getReturnType() == String.class && method.getParameterCount() == 0) {
                        if (method.getName().equals("getName")) {
                            return "TestPlayer";
                        }
                    }
                    if (method.getReturnType().isPrimitive()) {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == long.class) return 0L;
                        if (method.getReturnType() == float.class) return 0f;
                        if (method.getReturnType() == double.class) return 0d;
                        if (method.getReturnType() == short.class) return (short) 0;
                        if (method.getReturnType() == byte.class) return (byte) 0;
                        if (method.getReturnType() == char.class) return (char) 0;
                    }
                    return null;
                }
        );
    }

    private static final class TestManager extends MultiblockManager {
        private final MultiblockInstance instance;

        private TestManager(MultiblockInstance instance) {
            this.instance = instance;
        }

        @Override
        public Optional<MultiblockInstance> getInstanceAt(Location loc) {
            return Optional.of(instance);
        }
    }
}
