package de.diddiz.LogBlock;

import org.bukkit.inventory.ItemStack;

public class ChestAccess {
    final ItemStack itemStack;
    final boolean remove;
    final int itemType;

    public ChestAccess(ItemStack itemStack, boolean remove, int itemType) {
        this.itemStack = itemStack;
        this.remove = remove;
        this.itemType = itemType;
    }
}
