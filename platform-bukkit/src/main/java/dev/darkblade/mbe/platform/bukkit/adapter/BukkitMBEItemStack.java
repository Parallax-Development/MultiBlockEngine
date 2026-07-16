package dev.darkblade.mbe.platform.bukkit.adapter;

import dev.darkblade.mbe.api.platform.MBEItemStack;
import org.bukkit.inventory.ItemStack;

public class BukkitMBEItemStack implements MBEItemStack {
    
    private final ItemStack bukkitItemStack;
    
    public BukkitMBEItemStack(ItemStack bukkitItemStack) {
        this.bukkitItemStack = bukkitItemStack;
    }
    
    @Override
    public String getType() {
        if (bukkitItemStack == null || bukkitItemStack.getType().isAir()) {
            return "minecraft:air";
        }
        return bukkitItemStack.getType().getKey().toString();
    }
    
    @Override
    public int getAmount() {
        if (bukkitItemStack == null) {
            return 0;
        }
        return bukkitItemStack.getAmount();
    }
    
    @Override
    public void setAmount(int amount) {
        if (bukkitItemStack != null) {
            bukkitItemStack.setAmount(amount);
        }
    }
    
    @Override
    public boolean isEmpty() {
        return bukkitItemStack == null || bukkitItemStack.getType().isAir();
    }
    
    public ItemStack getBukkitItemStack() {
        return bukkitItemStack;
    }
}
