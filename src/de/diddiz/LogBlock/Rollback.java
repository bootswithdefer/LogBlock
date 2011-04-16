package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.Selection;

public class Rollback implements Runnable
{
	private Player player;
	private Connection conn;
	private LogBlock logblock;
	private String query;
	private boolean redo;

	Rollback(Player player, Connection conn, LogBlock logblock, String name, int radius, Selection sel, int minutes, String table, boolean redo) {
		this.player = player;
		this.conn = conn;
		this.logblock = logblock;
		this.redo = redo;
		if (!redo)
			query = "SELECT replaced, type, data, x, y, z ";
		else
			query = "SELECT type AS replaced, replaced AS type, data, x, y, z ";
		query += "FROM `" + table + "` INNER JOIN `lb-players` USING (playerid) WHERE (type <> replaced OR (type = 0 AND replaced = 0)) AND ";
		if (name != null)
			query += "playername = '" + name + "' AND ";
		if (radius != -1)
			query += "x > '" + (player.getLocation().getBlockX() - radius) + "' AND x < '" + (player.getLocation().getBlockX() + radius) + "' AND z > '" + (player.getLocation().getBlockZ() - radius) + "' AND z < '" + (player.getLocation().getBlockZ() + radius) + "' AND ";
		if (sel != null)
			query += "x >= '"+ Math.min(sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX()) + "' AND x <= '" + Math.max(sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX()) + "' AND y >= '" + Math.min(sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY()) + "' AND y <= '" + Math.max(sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY()) + "' AND z >= '" + Math.min(sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ()) + "' AND z <= '" + Math.max(sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ()) + "' AND ";
		if (minutes >= 0)
			query += "date > date_sub(now(), INTERVAL " + minutes + " MINUTE) AND ";
		query = query.substring(0, query.length() - 4);
		if (!redo)
			query += "ORDER BY date DESC, id DESC";
		else
			query += "ORDER BY date ASC, id ASC";
	}

	public void run() {
		ResultSet rs = null;
		LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();
		edits.clear();
		try {
			rs = conn.createStatement().executeQuery(query);
			while (rs.next()) {
				Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), player.getWorld());
				edits.offer(e);
			}
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, "[LogBlock Rollback] SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock Rollback] SQL exception on close", ex);
				player.sendMessage(ChatColor.RED + "Error, check server logs.");
				return;
			}
		}
		int changes = edits.size();
		player.sendMessage(ChatColor.GREEN + "" + changes + " Changes found.");
		PerformRollback perform = new PerformRollback(edits, this);
		long start = System.currentTimeMillis();
		int taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, perform, 0, 1);
		if (taskID == -1) {
			player.sendMessage(ChatColor.RED + "Failed to schedule rollback task");
			return;
		}
		synchronized (this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				LogBlock.log.severe("[LogBlock Rollback] Interrupted");
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
		private LinkedBlockingQueue<Edit> edits;
		private Rollback rollback;
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
		int type, replaced;
		int x, y, z;
		byte data;
		World world;

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
			if (logblock.config.dontRollback.contains(replaced))
				return PerformResult.BLACKLISTED;
			try {
				Block block = world.getBlockAt(x, y, z);
				if (!world.isChunkLoaded(block.getChunk()))
					world.loadChunk(block.getChunk());
				if (equalsType(block.getTypeId(), type) || logblock.config.replaceAnyway.contains(block.getTypeId()) || (type == 0 && replaced == 0)) {
					if (block.setTypeIdAndData(replaced, data, false))
						return PerformResult.SUCCESS;
					else
						return PerformResult.ERROR;
				} else
					return PerformResult.NO_ACTION;
			} catch (Exception ex) {
				LogBlock.log.severe("[LogBlock Rollback] " + ex.toString());
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