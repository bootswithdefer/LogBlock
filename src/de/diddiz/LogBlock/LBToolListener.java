package de.diddiz.LogBlock;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

class LBToolListener extends PlayerListener
{
	private final LogBlock logblock;
	private final Config config;
	private final CommandsHandler handler;

	LBToolListener(LogBlock logblock) {
		this.logblock = logblock;
		config = logblock.getConfig();
		handler = logblock.getCommandsHandler();
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled()) {
			final Player player = event.getPlayer();
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == config.toolID && logblock.hasPermission(player, "logblock.tool") && logblock.getSession(player.getName()).toolEnabled) {
				if (config.tables.get(player.getWorld().getName().hashCode()) != null) {
					try {
						final QueryParams params = logblock.getSession(player.getName()).toolQuery;
						params.loc = event.getClickedBlock().getLocation();
						params.world = player.getWorld();
						handler.new CommandLookup(player, params);
					} catch (final Exception ex) {
						player.sendMessage(ChatColor.RED + ex.getMessage());
					}
					if (event.getClickedBlock().getTypeId() != 26)
						event.setCancelled(true);
				} else
					player.sendMessage("This world isn't logged");
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == config.toolblockID && logblock.hasPermission(player, "logblock.toolblock") && logblock.getSession(player.getName()).toolBlockEnabled)
				if (config.tables.get(player.getWorld().getName().hashCode()) != null) {
					try {
						final QueryParams params = logblock.getSession(player.getName()).toolQuery;
						params.loc = event.getClickedBlock().getFace(event.getBlockFace()).getLocation();
						params.world = player.getWorld();
						handler.new CommandLookup(player, params);
					} catch (final Exception ex) {
						player.sendMessage(ChatColor.RED + ex.getMessage());
					}
					event.setCancelled(true);
				} else
					player.sendMessage("This world isn't logged");
		}
	}
}
