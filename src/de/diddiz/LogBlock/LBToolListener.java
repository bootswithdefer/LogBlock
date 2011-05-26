package de.diddiz.LogBlock;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

class LBToolListener extends PlayerListener
{
	private final CommandsHandler handler;
	private final LogBlock logblock;
	private final int toolID;
	private final int toolblockID;
	private final Map<Integer, String> tables;

	LBToolListener(LogBlock logblock) {
		this.logblock = logblock;
		handler = logblock.getCommandsHandler();
		toolID = logblock.getConfig().toolID;
		toolblockID = logblock.getConfig().toolblockID;
		tables = logblock.getConfig().tables;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled()) {
			final Player player = event.getPlayer();
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == toolID && logblock.hasPermission(player, "logblock.tool") && logblock.getSession(player.getName()).toolEnabled) {
				if (tables.get(player.getWorld().getName().hashCode()) != null) {
					try {
						final QueryParams params = logblock.getSession(player.getName()).toolQuery;
						params.setLocation(event.getClickedBlock().getLocation());
						handler.new CommandLookup(player, params);
					} catch (final Exception ex) {
						player.sendMessage(ChatColor.RED + ex.getMessage());
					}
					if (event.getClickedBlock().getTypeId() != 26)
						event.setCancelled(true);
				} else
					player.sendMessage("This world isn't logged");
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial().getId() == toolblockID && logblock.hasPermission(player, "logblock.toolblock") && logblock.getSession(player.getName()).toolBlockEnabled)
				if (tables.get(player.getWorld().getName().hashCode()) != null) {
					try {
						final QueryParams params = logblock.getSession(player.getName()).toolQuery;
						params.setLocation(event.getClickedBlock().getLocation());
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
