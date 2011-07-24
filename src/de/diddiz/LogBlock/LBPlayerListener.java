package de.diddiz.LogBlock;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

class LBPlayerListener extends PlayerListener
{
	private final Consumer consumer;

	LBPlayerListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	@Override
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (!event.isCancelled())
			if (event.getBucket() == Material.WATER_BUCKET)
				consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), 9, (byte)0);
			else if (event.getBucket() == Material.LAVA_BUCKET)
				consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), 11, (byte)0);
	}

	@Override
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (!event.isCancelled())
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlockClicked().getState());
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled() && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			final int type = event.getClickedBlock().getTypeId();
			if (type == 69 || type == 77)
				consumer.queueBlock(event.getPlayer().getName(), event.getClickedBlock().getLocation(), type, type, (byte)0);
		}
	}
}
