package de.diddiz.LogBlock;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

class LBPlayerListener extends PlayerListener
{
	private final Consumer consumer;
	private final Map<Integer, WorldConfig> worlds;

	LBPlayerListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
		worlds = logblock.getConfig().worlds;
	}

	@Override
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logBlockPlacings)
			consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), event.getBucket() == Material.WATER_BUCKET ? 9 : 11, (byte)0);
	}

	@Override
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logBlockBreaks)
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlockClicked().getState());
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			final int type = event.getClickedBlock().getTypeId();
			if (wcfg.logButtonsAndLevers && (type == 69 || type == 77))
				consumer.queueBlock(event.getPlayer().getName(), event.getClickedBlock().getLocation(), type, type, (byte)0);
			else if (wcfg.logDoors && (type == 64 || type == 96))
				consumer.queueBlock(event.getPlayer().getName(), event.getClickedBlock().getLocation(), type, type, (byte)((event.getClickedBlock().getData() & 4) / 4));
		}
	}

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logChat)
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.logChat)
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}
}
