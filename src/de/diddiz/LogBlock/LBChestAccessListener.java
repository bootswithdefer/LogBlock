package de.diddiz.LogBlock;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import de.diddiz.util.BukkitUtils;

class LBChestAccessListener extends PlayerListener
{
	private final Consumer consumer;
	private final HashMap<Integer, Container> containers = new HashMap<Integer, Container>();

	LBChestAccessListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	private void logChestAccess(String playerName) {
		final Container container = containers.get(playerName.hashCode());
		final Location loc = container.getLocation();
		final ItemStack[] after = BukkitUtils.compressInventory(((ContainerBlock)loc.getWorld().getBlockAt(loc).getState()).getInventory().getContents());
		final ItemStack[] diff = BukkitUtils.compareInventories(container.getContent(), after);
		for (final ItemStack item : diff)
			consumer.queueChestAccess(playerName, loc, container.getTypeId(), (short)item.getTypeId(), (short)item.getAmount(), BukkitUtils.rawData(item));
		containers.remove(playerName.hashCode());
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (containers.containsKey(event.getPlayer().getName().hashCode()))
			logChestAccess(event.getPlayer().getName());
		if (!event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			final BlockState state = event.getClickedBlock().getState();
			if (state instanceof ContainerBlock)
				containers.put(event.getPlayer().getName().hashCode(), new Container(state));
		}
	}

	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		final Container container = containers.get(event.getPlayer().getName().hashCode());
		if (container != null && !container.isSliding())
			logChestAccess(event.getPlayer().getName());
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (containers.containsKey(event.getPlayer().getName().hashCode()))
			logChestAccess(event.getPlayer().getName());
	}

	private static class Container
	{
		private final ItemStack[] content;
		private final BlockState state;
		private final long start;

		Container(BlockState state) {
			this.state = state;
			content = BukkitUtils.compressInventory(((ContainerBlock)state).getInventory().getContents());
			start = System.currentTimeMillis();
		}

		ItemStack[] getContent() {
			return content;
		}

		Location getLocation() {
			return new Location(state.getWorld(), state.getX(), state.getY(), state.getZ());
		}

		int getTypeId() {
			return state.getTypeId();
		}

		boolean isSliding() {
			return System.currentTimeMillis() - start < 500;
		}
	}
}
