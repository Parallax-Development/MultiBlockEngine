package dev.darkblade.mbe.core.infrastructure.compat;

import dev.darkblade.mbe.api.compat.InventoryCompatService;
import dev.darkblade.mbe.api.service.MBEService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class BukkitInventoryCompatService implements InventoryCompatService, MBEService {
    private static final String SERVICE_ID = "mbe:compat.inventory";

    private InventoryViewDelegate delegate;

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public void onLoad() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> viewClass = Class.forName("org.bukkit.inventory.InventoryView");
            
            // For InventoryEvent (covers InventoryClickEvent, InventoryCloseEvent, etc)
            MethodHandle getEventViewMethod = lookup.findVirtual(InventoryEvent.class, "getView", MethodType.methodType(viewClass));
            
            // For Player (getOpenInventory)
            MethodHandle getPlayerViewMethod = lookup.findVirtual(Player.class, "getOpenInventory", MethodType.methodType(viewClass));
            
            MethodHandle getTopInventoryMethod = lookup.findVirtual(viewClass, "getTopInventory", MethodType.methodType(Inventory.class));
            MethodHandle getTitleMethod = lookup.findVirtual(viewClass, "getTitle", MethodType.methodType(String.class));

            delegate = new ReflectionInventoryViewDelegate(getEventViewMethod, getPlayerViewMethod, getTopInventoryMethod, getTitleMethod);
        } catch (Throwable e) {
            delegate = new FallbackInventoryViewDelegate();
        }
    }

    @Override
    public Inventory topInventory(InventoryEvent event) {
        if (event == null) {
            return null;
        }
        if (delegate == null) {
            return null; // Safe fallback avoiding JVM class verification errors
        }
        return delegate.getTopInventory(event);
    }

    @Override
    public String viewTitle(InventoryEvent event) {
        if (event == null) {
            return "";
        }
        if (delegate == null) {
            return "";
        }
        return delegate.getTitle(event);
    }

    @Override
    public Inventory topInventory(Player player) {
        if (player == null) {
            return null;
        }
        if (delegate == null) {
            return null;
        }
        return delegate.getTopInventory(player);
    }

    @Override
    public String viewTitle(Player player) {
        if (player == null) {
            return "";
        }
        if (delegate == null) {
            return "";
        }
        return delegate.getTitle(player);
    }

    // Retained for any legacy internal calls that haven't been recompiled
    @Override
    public Inventory topInventory(InventoryClickEvent event) {
        return topInventory((InventoryEvent) event);
    }

    @Override
    public String viewTitle(InventoryClickEvent event) {
        return viewTitle((InventoryEvent) event);
    }

    private interface InventoryViewDelegate {
        Inventory getTopInventory(InventoryEvent event);
        String getTitle(InventoryEvent event);
        Inventory getTopInventory(Player player);
        String getTitle(Player player);
    }

    private static class ReflectionInventoryViewDelegate implements InventoryViewDelegate {
        private final MethodHandle getEventView;
        private final MethodHandle getPlayerView;
        private final MethodHandle getTopInventory;
        private final MethodHandle getTitle;

        public ReflectionInventoryViewDelegate(MethodHandle getEventView, MethodHandle getPlayerView, MethodHandle getTopInventory, MethodHandle getTitle) {
            this.getEventView = getEventView;
            this.getPlayerView = getPlayerView;
            this.getTopInventory = getTopInventory;
            this.getTitle = getTitle;
        }

        @Override
        public Inventory getTopInventory(InventoryEvent event) {
            try {
                Object view = getEventView.invoke(event);
                return (Inventory) getTopInventory.invoke(view);
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        public String getTitle(InventoryEvent event) {
            try {
                Object view = getEventView.invoke(event);
                return (String) getTitle.invoke(view);
            } catch (Throwable e) {
                return "";
            }
        }

        @Override
        public Inventory getTopInventory(Player player) {
            try {
                Object view = getPlayerView.invoke(player);
                return (Inventory) getTopInventory.invoke(view);
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        public String getTitle(Player player) {
            try {
                Object view = getPlayerView.invoke(player);
                return (String) getTitle.invoke(view);
            } catch (Throwable e) {
                return "";
            }
        }
    }

    private static class FallbackInventoryViewDelegate implements InventoryViewDelegate {
        @Override
        public Inventory getTopInventory(InventoryEvent event) {
            return null;
        }

        @Override
        public String getTitle(InventoryEvent event) {
            return "";
        }

        @Override
        public Inventory getTopInventory(Player player) {
            return null;
        }

        @Override
        public String getTitle(Player player) {
            return "";
        }
    }
}
