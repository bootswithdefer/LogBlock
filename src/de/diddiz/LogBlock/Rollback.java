package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Rollback implements Runnable
{

	PreparedStatement ps = null;
	private Player player;
	private Connection conn;
	private LogBlock logblock;

	Rollback(Player player, Connection conn, LogBlock logblock, String name, int minutes, String table) {
		this.player = player;
		this.conn = conn;
		this.logblock = logblock;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` INNER JOIN `lb-players` USING (`playerid`) WHERE playername = ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDER BY date DESC");
			ps.setString(1, name);
			ps.setInt(2, minutes);
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		}
	}

	Rollback(Player player, Connection conn, LogBlock logblock, String name, int radius, int minutes, String table) {
		this.player = player;
		this.conn = conn;
		this.logblock = logblock;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` INNER JOIN `lb-players` USING (`playerid`) WHERE playername = ? AND x > ? AND x < ? AND z > ? AND z < ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDER BY date DESC");
			ps.setString(1, name);
			ps.setInt(2, player.getLocation().getBlockX()-radius);
			ps.setInt(3, player.getLocation().getBlockX()+radius);
			ps.setInt(4, player.getLocation().getBlockZ()-radius);
			ps.setInt(5, player.getLocation().getBlockZ()+radius);
			ps.setInt(6, minutes);
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		}
	}

	Rollback(Player player, Connection conn, LogBlock logblock, int radius, int minutes, String table) {
		this.player = player;
		this.conn = conn;
		this.logblock = logblock;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` WHERE x > ? AND x < ? AND z > ? AND z < ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDER BY date DESC");
			ps.setInt(1, player.getLocation().getBlockX()-radius);
			ps.setInt(2, player.getLocation().getBlockX()+radius);
			ps.setInt(3, player.getLocation().getBlockZ()-radius);
			ps.setInt(4, player.getLocation().getBlockZ()+radius);
			ps.setInt(5, minutes);
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		}
	}

	Rollback(Player player, Connection conn, LogBlock logblock, Location loc1, Location loc2, int minutes, String table) {
		this.player = player;
		this.conn = conn;
		this.logblock = logblock;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` WHERE x >= ? AND x <= ? AND y >= ? AND y <= ? AND z >= ? AND z <= ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDER BY date DESC", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, Math.min(loc1.getBlockX(), loc2.getBlockX()));
			ps.setInt(2, Math.max(loc1.getBlockX(), loc2.getBlockX()));
			ps.setInt(3, Math.min(loc1.getBlockY(), loc2.getBlockY()));
			ps.setInt(4, Math.max(loc1.getBlockY(), loc2.getBlockY()));
			ps.setInt(5, Math.min(loc1.getBlockZ(), loc2.getBlockZ()));
			ps.setInt(6, Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
			ps.setInt(7, minutes);
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		}
	}

	public void run() {
		ResultSet rs = null;
		LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();
		edits.clear();
		try {
			rs = ps.executeQuery();
			while (rs.next()) {
				Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), player.getWorld());
				edits.offer(e);
			}
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage(ChatColor.RED + "§cError, check server logs.");
			return;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
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
		player.sendMessage(ChatColor.GREEN + "Rollback finished successfully");
		player.sendMessage(ChatColor.GREEN + "Undid " + perform.rolledBack + " of " + changes + " changes");
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