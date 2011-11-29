package de.diddiz.LogBlock;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

class LBPlayerListener extends PlayerListener
{
	private final Consumer consumer;
	private final Map<Integer, WorldConfig> worlds;

	LBPlayerListener(LogBlock logblock) {
		consumer = logblock.getConsumer();
		worlds = logblock.getLBConfig().worlds;
	}

	@Override
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.isLogging(Logging.BLOCKPLACE))
			consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), event.getBucket() == Material.WATER_BUCKET ? 9 : 11, (byte)0);
	}

	@Override
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && wcfg.isLogging(Logging.BLOCKBREAK))
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlockClicked().getState());
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (!event.isCancelled() && wcfg != null && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			final int type = event.getClickedBlock().getTypeId();
			final Player player = event.getPlayer();
			final Location loc = event.getClickedBlock().getLocation();
			switch (type) {
				case 69:
				case 77:
					if (wcfg.isLogging(Logging.SWITCHINTERACT))
						consumer.queueBlock(player.getName(), loc, type, type, (byte)0);
					break;
				case 107:
					if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
						break;
				case 64:
				case 96:
					if (wcfg.isLogging(Logging.DOORINTERACT))
						consumer.queueBlock(player.getName(), loc, type, type, (byte)((event.getClickedBlock().getData() & 4) / 4));
					break;
				case 92:
					if (wcfg.isLogging(Logging.CAKEEAT) && player.getFoodLevel() < 20)
						consumer.queueBlock(player.getName(), loc, 92, 92, (byte)0);
					break;
				case 25:
					if (wcfg.isLogging(Logging.NOTEBLOCKINTERACT))
						consumer.queueBlock(player.getName(), loc, 25, 25, (byte)0);
					break;
				case 93:
				case 94:
					if (wcfg.isLogging(Logging.DIODEINTERACT) && event.getAction() == Action.RIGHT_CLICK_BLOCK)
						consumer.queueBlock(player.getName(), loc, type, type, (byte)0);
					break;
			}
		}
	}

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (wcfg != null && wcfg.isLogging(Logging.CHAT))
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		final WorldConfig wcfg = worlds.get(event.getPlayer().getWorld().getName().hashCode());
		if (wcfg != null && wcfg.isLogging(Logging.CHAT))
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		consumer.queueJoin(event.getPlayer());
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		consumer.queueLeave(event.getPlayer());
	}
}
