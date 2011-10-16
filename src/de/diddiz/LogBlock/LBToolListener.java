package de.diddiz.LogBlock;

import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;

class LBToolListener extends PlayerListener
{
	private final CommandsHandler handler;
	private final LogBlock logblock;
	private final Map<Integer, Tool> toolsByType;
	private final Map<Integer, WorldConfig> worlds;

	LBToolListener(LogBlock logblock) {
		this.logblock = logblock;
		handler = logblock.getCommandsHandler();
		worlds = logblock.getLBConfig().worlds;
		toolsByType = logblock.getLBConfig().toolsByType;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled() && event.getMaterial() != null) {
			final Action action = event.getAction();
			final int type = event.getMaterial().getId();
			final Tool tool = toolsByType.get(type);
			final Player player = event.getPlayer();
			if (tool != null && (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) && worlds.containsKey(player.getWorld().getName().hashCode()) && logblock.hasPermission(player, "logblock.tools." + tool.name)) {
				final ToolBehavior behavior = action == Action.RIGHT_CLICK_BLOCK ? tool.rightClickBehavior : tool.leftClickBehavior;
				final ToolData toolData = logblock.getSession(player.getName()).toolData.get(tool);
				if (behavior != ToolBehavior.NONE && toolData.enabled) {
					final Block block = event.getClickedBlock();
					final QueryParams params = toolData.params;
					params.loc = null;
					params.sel = null;
					if (behavior == ToolBehavior.BLOCK)
						params.setLocation(block.getRelative(event.getBlockFace()).getLocation());
					else if (block.getTypeId() != 54 || tool.params.radius != 0)
						params.setLocation(block.getLocation());
					else {
						for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST})
							if (block.getRelative(face).getTypeId() == 54)
								params.setSelection(new CuboidSelection(event.getPlayer().getWorld(), block.getLocation(), block.getRelative(face).getLocation()));
						if (params.sel == null)
							params.setLocation(block.getLocation());
					}
					try {
						if (toolData.mode == ToolMode.ROLLBACK)
							handler.new CommandRollback(player, params, true);
						else if (toolData.mode == ToolMode.REDO)
							handler.new CommandRedo(player, params, true);
						else if (toolData.mode == ToolMode.CLEARLOG)
							handler.new CommandClearLog(player, params, true);
						else if (toolData.mode == ToolMode.WRITELOGFILE)
							handler.new CommandWriteLogFile(player, params, true);
						else
							handler.new CommandLookup(player, params, true);
					} catch (final Exception ex) {
						player.sendMessage(ChatColor.RED + ex.getMessage());
					}
					event.setCancelled(true);
				}
			}
		}
	}

	@Override
	public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
		final String[] split = event.getMessage().split(" ");
		if (split.length > 1 && split[0].equalsIgnoreCase("/ban") && logblock.hasPermission(event.getPlayer(), logblock.getLBConfig().banPermission)) {
			final QueryParams p = new QueryParams(logblock);
			p.setPlayer(split[1].equalsIgnoreCase("g") ? split[2] : split[1]);
			p.since = 0;
			p.silent = false;
			logblock.getServer().getScheduler().scheduleAsyncDelayedTask(logblock, new Runnable() {
				@Override
				public void run() {
					for (final World world : logblock.getServer().getWorlds())
						if (worlds.get(world.getName().hashCode()) != null) {
							p.world = world;
							try {
								handler.new CommandRollback(event.getPlayer(), p, false);
							} catch (final Exception ex) {}
						}
				}
			});
		}
	}

	@Override
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		final Player player = event.getPlayer();
		final Session session = logblock.getSessions().get(player.getName().hashCode());
		if (session != null)
			for (final Entry<Tool, ToolData> entry : session.toolData.entrySet()) {
				final Tool tool = entry.getKey();
				final ToolData toolData = entry.getValue();
				if (toolData.enabled && !logblock.hasPermission(player, "logblock.tools." + tool.name)) {
					toolData.enabled = false;
					player.getInventory().removeItem(new ItemStack(tool.item, 1));
					player.sendMessage(ChatColor.GREEN + "Tool disabled.");
				}
			}
	}
}
