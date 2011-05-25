package de.diddiz.LogBlock;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkitcontrib.event.inventory.InventoryCloseEvent;
import org.bukkitcontrib.event.inventory.InventoryListener;
import org.bukkitcontrib.event.inventory.InventoryOpenEvent;
import de.diddiz.util.BukkitUtils;

class LBChestAccessListener extends InventoryListener
{
	private final Consumer consumer;
	private final HashMap<Integer, ItemStack[]> containers = new HashMap<Integer, ItemStack[]>();

	LBChestAccessListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	@Override
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!event.isCancelled() && event.getLocation() != null && containers.containsKey(event.getPlayer().getName().hashCode())) {
			final String playerName = event.getPlayer().getName();
			final Location loc = event.getLocation();
			final ItemStack[] before = containers.get(playerName.hashCode());
			final ItemStack[] after = BukkitUtils.compressInventory(event.getInventory().getContents());
			final ItemStack[] diff = BukkitUtils.compareInventories(before, after);
			for (final ItemStack item : diff)
				consumer.queueChestAccess(playerName, loc, loc.getWorld().getBlockTypeIdAt(loc), (short)item.getTypeId(), (short)item.getAmount(), BukkitUtils.rawData(item));
			containers.remove(playerName.hashCode());
		}
	}

	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!event.isCancelled() && event.getLocation() != null) {
			final BlockState state = event.getLocation().getWorld().getBlockAt(event.getLocation()).getState();
			if (state instanceof ContainerBlock)
				containers.put(event.getPlayer().getName().hashCode(), BukkitUtils.compressInventory(((ContainerBlock)state).getInventory().getContents()));
		}
	}
}
