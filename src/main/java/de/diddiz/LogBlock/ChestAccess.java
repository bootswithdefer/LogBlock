package de.diddiz.LogBlock;

import org.bukkit.inventory.ItemStack;

public class ChestAccess {
    public final ItemStack itemStack;
    public final boolean remove;
    public final int itemType;

    public ChestAccess(ItemStack itemStack, boolean remove, int itemType) {
        this.itemStack = itemStack;
        this.remove = remove;
        this.itemType = itemType;
    }
}
