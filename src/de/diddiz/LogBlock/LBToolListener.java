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
			final Action action = event.getAction();
			final int type = event.getMaterial().getId();
			if (type == toolID && action == Action.RIGHT_CLICK_BLOCK || type == toolblockID && (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK)) {
				final Player player = event.getPlayer();
				final Session session = logblock.getSession(player.getName());
				if (type == toolID && session.toolEnabled && logblock.hasPermission(player, "logblock.tool") || type == toolblockID && session.toolBlockEnabled && logblock.hasPermission(player, "logblock.toolblock"))
					if (tables.get(player.getWorld().getName().hashCode()) != null) {
						if (!(type == toolID && event.getClickedBlock().getTypeId() == 26))
							event.setCancelled(true);
						final QueryParams params = type == toolID ? session.toolQuery : session.toolBlockQuery;
						final ToolMode mode = type == toolID ? session.toolMode : session.toolBlockMode;
						if (type == toolblockID && action == Action.RIGHT_CLICK_BLOCK)
							params.setLocation(event.getClickedBlock().getFace(event.getBlockFace()).getLocation());
						else
							params.setLocation(event.getClickedBlock().getLocation());
						try {
							if (mode == ToolMode.ROLLBACK)
								handler.new CommandRollback(player, params);
							else if (mode == ToolMode.REDO)
								handler.new CommandRedo(player, params);
							else if (mode == ToolMode.CLEARLOG)
								handler.new CommandClearLog(player, params);
							else if (mode == ToolMode.WRITELOGFILE)
								handler.new CommandWriteLogFile(player, params);
							else
								handler.new CommandLookup(player, params);
						} catch (final Exception ex) {
							player.sendMessage(ChatColor.RED + ex.getMessage());
						}
					} else
						player.sendMessage("This world isn't logged");
			}
		}
	}
}
