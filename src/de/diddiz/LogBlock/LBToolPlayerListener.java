package de.diddiz.LogBlock;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class LBToolPlayerListener extends PlayerListener
{
	private final LogBlock logblock;
	private final Config config;

	LBToolPlayerListener(LogBlock logblock) {
		this.logblock = logblock;
		config = logblock.getConfig();
	}

	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled()) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == config.toolID && logblock.checkPermission(event.getPlayer(), "logblock.lookup")) {
				logblock.getServer().getScheduler().scheduleAsyncDelayedTask(logblock, new BlockStats(logblock, event.getPlayer(), event.getClickedBlock()));
				if (event.getClickedBlock().getType() != Material.BED_BLOCK)
					event.setCancelled(true);
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == config.toolblockID && logblock.checkPermission(event.getPlayer(), "logblock.lookup")) {
				logblock.getServer().getScheduler().scheduleAsyncDelayedTask(logblock, new BlockStats(logblock, event.getPlayer(), event.getClickedBlock().getFace(event.getBlockFace())));
				event.setCancelled(true);
			}
		}
	}
}
