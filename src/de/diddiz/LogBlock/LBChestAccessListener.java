package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.compareInventories;
import static de.diddiz.util.BukkitUtils.compressInventory;
import static de.diddiz.util.BukkitUtils.rawData;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkitcontrib.event.inventory.InventoryCloseEvent;
import org.bukkitcontrib.event.inventory.InventoryListener;
import org.bukkitcontrib.event.inventory.InventoryOpenEvent;

class LBChestAccessListener extends InventoryListener
{
	private final Consumer consumer;
	private final Map<Integer, ItemStack[]> containers = new HashMap<Integer, ItemStack[]>();

	LBChestAccessListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	@Override
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!event.isCancelled() && event.getLocation() != null && containers.containsKey(event.getPlayer().getName().hashCode())) {
			final String playerName = event.getPlayer().getName();
			final Location loc = event.getLocation();
			final ItemStack[] before = containers.get(playerName.hashCode());
			final ItemStack[] after = compressInventory(event.getInventory().getContents());
			final ItemStack[] diff = compareInventories(before, after);
			for (final ItemStack item : diff)
				consumer.queueChestAccess(playerName, loc, loc.getWorld().getBlockTypeIdAt(loc), (short)item.getTypeId(), (short)item.getAmount(), rawData(item));
			containers.remove(playerName.hashCode());
		}
	}

	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!event.isCancelled() && event.getLocation() != null)
			containers.put(event.getPlayer().getName().hashCode(), compressInventory(event.getInventory().getContents()));
	}
}
