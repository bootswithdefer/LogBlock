package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.Selection;

public class Rollback implements Runnable
{
	private final Logger log;
	private final LogBlock logblock;
	private final Config config;
	private final Player player;
	private final Connection conn;
	private final String query;
	private final boolean redo;

	Rollback(LogBlock logblock, Player player, String name, int radius, Selection sel, int minutes, boolean redo) {
		this.logblock = logblock;
		this.player = player;
		this.redo = redo;
		log = logblock.getServer().getLogger();
		conn = logblock.getConnection();
		config = logblock.getConfig();
		final StringBuffer sql = new StringBuffer();
		if (!redo)
			sql.append("SELECT replaced, type, data, x, y, z ");
		else
			sql.append("SELECT type AS replaced, replaced AS type, data, x, y, z ");
		sql.append("FROM `" + logblock.getConfig().tables.get(player.getWorld().getName().hashCode()) + "` INNER JOIN `lb-players` USING (playerid) WHERE (type <> replaced OR (type = 0 AND replaced = 0)) AND ");
		if (name != null)
			sql.append("playername = '" + name + "' AND ");
		if (radius != -1)
			sql.append("x > '" + (player.getLocation().getBlockX() - radius) + "' AND x < '" + (player.getLocation().getBlockX() + radius) + "' AND z > '" + (player.getLocation().getBlockZ() - radius) + "' AND z < '" + (player.getLocation().getBlockZ() + radius) + "' AND ");
		if (sel != null)
			sql.append("x >= '"+ Math.min(sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX()) + "' AND x <= '" + Math.max(sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX()) + "' AND y >= '" + Math.min(sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY()) + "' AND y <= '" + Math.max(sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY()) + "' AND z >= '" + Math.min(sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ()) + "' AND z <= '" + Math.max(sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ()) + "' AND ");
		if (minutes >= 0)
			sql.append("date > date_sub(now(), INTERVAL " + minutes + " MINUTE) AND ");
		sql.delete(sql.length() - 5, sql.length() - 1);
		if (!redo)
			sql.append("ORDER BY date DESC, id DESC");
		else
			sql.append("ORDER BY date ASC, id ASC");
		query = sql.toString();
	}

	@Override
	public void run() {
		ResultSet rs = null;
		final LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();
		edits.clear();
		try {
			if (!config.tables.containsKey(player.getWorld().getName().hashCode())) {
				player.sendMessage(ChatColor.RED +  "This world isn't logged!");
				return;
			}
			rs = conn.createStatement().executeQuery(query);
			while (rs.next()) {
				final Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), player.getWorld());
				edits.offer(e);
			}
		} catch (final SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock Rollback] SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock Rollback] SQL exception on close", ex);
				player.sendMessage(ChatColor.RED + "Error, check server logs.");
				return;
			}
		}
		final int changes = edits.size();
		player.sendMessage(ChatColor.GREEN + "" + changes + " Changes found.");
		final PerformRollback perform = new PerformRollback(edits, this);
		final long start = System.currentTimeMillis();
		final int taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, perform, 0, 1);
		if (taskID == -1) {
			player.sendMessage(ChatColor.RED + "Failed to schedule rollback task");
			return;
		}
		synchronized (this) {
			try {
				this.wait();
			} catch (final InterruptedException e) {
				log.severe("[LogBlock Rollback] Interrupted");
			}
		}
		logblock.getServer().getScheduler().cancelTask(taskID);
		if (!redo) {
			player.sendMessage(ChatColor.GREEN + "Rollback finished successfully");
			player.sendMessage(ChatColor.GREEN + "Undid " + perform.successes + " of " + changes + " changes (" + perform.errors + " errors, " + perform.blacklisteds + " blacklist collisions)");
		} else {
			player.sendMessage(ChatColor.GREEN + "Redo finished successfully");
			player.sendMessage(ChatColor.GREEN + "Redid " + perform.successes + " of " + changes + " changes (" + perform.errors + " errors, " + perform.blacklisteds + " blacklist collisions)");
		}
		player.sendMessage(ChatColor.GREEN + "Took:  " + (System.currentTimeMillis() - start) + "ms");
	}

	private class PerformRollback implements Runnable
	{
		private final LinkedBlockingQueue<Edit> edits;
		private final Rollback rollback;
		int successes = 0;
		int errors = 0;
		int blacklisteds = 0;

		PerformRollback(LinkedBlockingQueue<Edit> edits, Rollback rollback) {
			this.edits = edits;
			this.rollback = rollback;
		}

		@Override
		public void run() {
			int counter = 0;
			while (!edits.isEmpty() && counter < 1000)
			{
				switch (edits.poll().perform()) {
				case SUCCESS:
					successes++;
					break;
				case ERROR:
					errors++;
					break;
				case BLACKLISTED:
					blacklisteds++;
					break;
				}
				counter++;
			}
			if (edits.isEmpty()) {
				synchronized (rollback) {
					rollback.notify();
				}
			}
		}
	}

	private enum PerformResult {
		ERROR, SUCCESS, BLACKLISTED, NO_ACTION
	}

	private class Edit
	{
		final int type, replaced;
		final int x, y, z;
		final byte data;
		final World world;

		Edit(int type, int replaced, byte data, int x, int y, int z, World world) {
			this.type = type;
			this.replaced = replaced;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.world = world;
		}

		private PerformResult perform() {
			if (config.dontRollback.contains(replaced))
				return PerformResult.BLACKLISTED;
			try {
				final Block block = world.getBlockAt(x, y, z);
				if (!world.isChunkLoaded(block.getChunk()))
					world.loadChunk(block.getChunk());
				if (equalsType(block.getTypeId(), type) || config.replaceAnyway.contains(block.getTypeId()) || type == 0 && replaced == 0) {
					if (block.setTypeIdAndData(replaced, data, false))
						return PerformResult.SUCCESS;
					else
						return PerformResult.ERROR;
				} else
					return PerformResult.NO_ACTION;
			} catch (final Exception ex) {
				log.severe("[LogBlock Rollback] " + ex.toString());
				return PerformResult.ERROR;
			}
		}

		private boolean equalsType(int type1, int type2) {
			if ((type1 == 2 || type1 == 3) && (type2 == 2 || type2 == 3))
				return true;
			if (type1 == type2)
				return true;
			return false;
		}
	}
}