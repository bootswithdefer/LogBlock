package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.BukkitUtils.*;

public class ChestAccessLogging extends LoggingListener {
    private class PlayerActiveInventoryModifications {
        private HashMap<ItemStack, Integer> modifications;

        public PlayerActiveInventoryModifications() {
            this.modifications = new HashMap<>();
        }

        public void addModification(ItemStack stack, int amount) {
            if (amount == 0) {
                return;
            }
            consumer.getLogblock().getLogger().info("Modify container: " + stack + " change: " + amount);
            stack = new ItemStack(stack);
            stack.setAmount(1);
            Integer existing = modifications.get(stack);
            int newTotal = amount + (existing == null ? 0 : existing);
            if (newTotal == 0) {
                modifications.remove(stack);
            } else {
                modifications.put(stack, newTotal);
            }
        }

        public HashMap<ItemStack, Integer> getModifications() {
            return modifications;
        }
    }

    private final Map<HumanEntity, PlayerActiveInventoryModifications> containers = new HashMap<>();

    public ChestAccessLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        final HumanEntity player = event.getPlayer();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final PlayerActiveInventoryModifications modifications = containers.remove(player);
            if (modifications != null) {
                final Location loc = getInventoryHolderLocation(holder);
                if (loc != null) {
                    for (Entry<ItemStack, Integer> e : modifications.getModifications().entrySet()) {
                        ItemStack stack = e.getKey();
                        int amount = e.getValue();
                        stack.setAmount(Math.abs(amount));
                        consumer.getLogblock().getLogger().info("Store container: " + stack + " take: " + (amount < 0));
                        consumer.queueChestAccess(Actor.actorFromEntity(player), loc, loc.getWorld().getBlockAt(loc).getBlockData(), stack, amount < 0);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        final HumanEntity player = event.getPlayer();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        if (event.getInventory() != null) {
            InventoryHolder holder = event.getInventory().getHolder();
            if (holder instanceof BlockState || holder instanceof DoubleChest) {
                if (getInventoryHolderType(holder) != Material.CRAFTING_TABLE) {
                    containers.put(player, new PlayerActiveInventoryModifications());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        final HumanEntity player = event.getWhoClicked();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final PlayerActiveInventoryModifications modifications = containers.get(player);
            if (modifications != null) {
                switch (event.getAction()) {
                    case PICKUP_ONE:
                    case DROP_ONE_SLOT:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCurrentItem(), -1);
                        }
                        break;
                    case PICKUP_HALF:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCurrentItem(), -(event.getCurrentItem().getAmount() + 1) / 2);
                        }
                        break;
                    case PICKUP_SOME: // oversized stack - can not take all when clicking
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            int taken = event.getCurrentItem().getAmount() - event.getCurrentItem().getMaxStackSize();
                            modifications.addModification(event.getCursor(), -taken);
                        }
                        break;
                    case PICKUP_ALL:
                    case DROP_ALL_SLOT:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                        }
                        break;
                    case PLACE_ONE:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCursor(), 1);
                        }
                        break;
                    case PLACE_SOME: // not enough free place in target slot
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            int placeable = event.getCurrentItem().getMaxStackSize() - event.getCurrentItem().getAmount();
                            modifications.addModification(event.getCursor(), placeable);
                        }
                        break;
                    case PLACE_ALL:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCursor(), event.getCursor().getAmount());
                        }
                        break;
                    case SWAP_WITH_CURSOR:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            modifications.addModification(event.getCursor(), event.getCursor().getAmount());
                            modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                        }
                        break;
                    case MOVE_TO_OTHER_INVENTORY: // shift + click
                        boolean removed = event.getRawSlot() < event.getView().getTopInventory().getSize();
                        modifications.addModification(event.getCurrentItem(), event.getCurrentItem().getAmount() * (removed ? -1 : 1));
                        break;
                    case COLLECT_TO_CURSOR: // double click
                        // first collect all with an amount != maxstacksize, then others, starting from slot 0 (container)
                        ItemStack cursor = event.getCursor();
                        if (cursor == null) {
                            return;
                        }
                        int toPickUp = cursor.getMaxStackSize() - cursor.getAmount();
                        int takenFromContainer = 0;
                        boolean takeFromFullStacks = false;
                        Inventory top = event.getView().getTopInventory();
                        Inventory bottom = event.getView().getBottomInventory();
                        while (true) {
                            for (ItemStack stack : top.getStorageContents()) {
                                if (cursor.isSimilar(stack)) {
                                    if (takeFromFullStacks == (stack.getAmount() == stack.getMaxStackSize())) {
                                        consumer.getLogblock().getLogger().info("Collect, from " + stack + ": " + takeFromFullStacks + ";" + stack.getAmount() + ";" + stack.getMaxStackSize());
                                        int take = Math.min(toPickUp, stack.getAmount());
                                        toPickUp -= take;
                                        takenFromContainer += take;
                                        if (toPickUp <= 0) {
                                            break;
                                        }
                                    }
                                }
                            }
                            for (ItemStack stack : bottom.getStorageContents()) {
                                if (cursor.isSimilar(stack)) {
                                    if (takeFromFullStacks == (stack.getAmount() == stack.getMaxStackSize())) {
                                        consumer.getLogblock().getLogger().info("Collect 2, from " + stack + ": " + takeFromFullStacks + ";" + stack.getAmount() + ";" + stack.getMaxStackSize());
                                        int take = Math.min(toPickUp, stack.getAmount());
                                        toPickUp -= take;
                                        if (toPickUp <= 0) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (takeFromFullStacks) {
                                break;
                            } else {
                                takeFromFullStacks = true;
                            }
                        }
                        if (takenFromContainer > 0) {
                            modifications.addModification(event.getCursor(), -takenFromContainer);
                        }
                        break;
                    case HOTBAR_SWAP: // number key or offhand key
                    case HOTBAR_MOVE_AND_READD:
                        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                            ItemStack otherSlot = (event.getClick() == ClickType.SWAP_OFFHAND) ? event.getWhoClicked().getInventory().getItemInOffHand() : event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                                modifications.addModification(event.getCurrentItem(), -event.getCurrentItem().getAmount());
                            }
                            if (otherSlot != null && otherSlot.getType() != Material.AIR) {
                                modifications.addModification(otherSlot, otherSlot.getAmount());
                            }
                        }
                        break;
                    case DROP_ALL_CURSOR:
                    case DROP_ONE_CURSOR:
                    case CLONE_STACK:
                    case NOTHING:
                        // only the cursor or nothing (but not the inventory) was modified
                        break;
                    case UNKNOWN:
                    default:
                        // unable to log something we don't know
                        consumer.getLogblock().getLogger().warning("Unknown inventory action by " + event.getWhoClicked().getName() + ": " + event.getAction() + " Slot: " + event.getSlot() + " Slot type: " + event.getSlotType());
                        break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryDragEvent event) {
        final HumanEntity player = event.getWhoClicked();
        if (!isLogging(player.getWorld(), Logging.CHESTACCESS)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final PlayerActiveInventoryModifications modifications = containers.get(player);
            if (modifications != null) {
                Inventory container = event.getView().getTopInventory();
                int containerSize = container.getSize();
                for (Entry<Integer, ItemStack> e : event.getNewItems().entrySet()) {
                    int slot = e.getKey();
                    if (slot < containerSize) {
                        ItemStack old = container.getItem(slot);
                        int oldAmount = (old == null || old.getType() == Material.AIR) ? 0 : old.getAmount();
                        modifications.addModification(e.getValue(), e.getValue().getAmount() - oldAmount);
                    }
                }
            }
        }
    }
}
