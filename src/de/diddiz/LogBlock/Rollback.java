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
			query = "SELECT replaced, type, data, x, y, z FROM `" + table + "` INNER JOIN `lb-players` USING (playerid) WHERE ";
		else
			query = "SELECT type AS replaced, replaced AS type, data, x, y, z FROM `" + table + "` INNER JOIN `lb-players` USING (playerid) WHERE ";
		if (name != null)
			query += "playername = '" + name + "' AND ";
		if (radius != -1)
			query += "x > '" + (player.getLocation().getBlockX() - radius)
				+ "' AND x < '" + (player.getLocation().getBlockX() + radius)
				+ "' AND z > '" + (player.getLocation().getBlockZ() - radius)
				+ "' AND z < '" + (player.getLocation().getBlockZ() + radius) + "' AND ";
		if (sel != null)
			query += "x >= '"+ Math.min(sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX())
				+ "' AND x <= '" + Math.max(sel.getMinimumPoint().getBlockX(), sel.getMaximumPoint().getBlockX())
				+ "' AND y >= '" + Math.min(sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY())
				+ "' AND y <= '" + Math.max(sel.getMinimumPoint().getBlockY(), sel.getMaximumPoint().getBlockY())
				+ "' AND z >= '" + Math.min(sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ())
				+ "' AND z <= '" + Math.max(sel.getMinimumPoint().getBlockZ(), sel.getMaximumPoint().getBlockZ()) + "' AND ";
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
			player.sendMessage(ChatColor.GREEN + "Undid " + perform.rolledBack + " of " + changes + " changes");
		} else {
			player.sendMessage(ChatColor.GREEN + "Redo finished successfully");
			player.sendMessage(ChatColor.GREEN + "Redid " + perform.rolledBack + " of " + changes + " changes");
		}
		player.sendMessage(ChatColor.GREEN + "Took:  " + (System.currentTimeMillis() - start) + "ms");
	}

	private class PerformRollback implements Runnable
	{
		private LinkedBlockingQueue<Edit> edits;
		private Rollback rollback;
		int rolledBack = 0;

		PerformRollback(LinkedBlockingQueue<Edit> edits, Rollback rollback) {
			this.edits = edits;
			this.rollback = rollback;
		}

		@Override
		public void run() {
			int counter = 0;
			while (!edits.isEmpty() && counter < 1000)
			{
				if (edits.poll().perform())
					rolledBack++;
				counter++;
			}
			if (edits.isEmpty()) {
				synchronized (rollback) {
					rollback.notify();
				}
			}
		}
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

		public boolean perform() {
			if (type > 0 && type == replaced)
				return false;
			try {
				Block block = world.getBlockAt(x, y, z);
				if (!world.isChunkLoaded(block.getChunk()))
					world.loadChunk(block.getChunk());
				if (block.getTypeId() == type || (block.getTypeId() >= 8 && block.getTypeId() <= 11) || block.getTypeId() == 51 || (type == 0 && replaced == 0))
					return block.setTypeIdAndData(replaced, data, false);
			} catch (Exception ex) {
					LogBlock.log.severe("[LogBlock Rollback] " + ex.toString());
			}
			return false;
		}
	}
}