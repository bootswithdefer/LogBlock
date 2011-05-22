package de.diddiz.LogBlock;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerListener;

class LBPlayerListener extends PlayerListener
{
	private final Consumer consumer;

	LBPlayerListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
	}

	@Override
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (!event.isCancelled())
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlockClicked().getState());
	}

	@Override
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (event.getBucket() == Material.WATER_BUCKET)
			consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()).getLocation(), 9, (byte)0);
		else if (event.getBucket() == Material.LAVA_BUCKET)
			consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getFace(event.getBlockFace()).getLocation(), 11, (byte)0);
	}
}
