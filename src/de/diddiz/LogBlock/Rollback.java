package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Rollback implements Runnable
{
	private LinkedBlockingQueue<Edit> edits = new LinkedBlockingQueue<Edit>();
	PreparedStatement ps = null;
	private Player player;
	private Connection conn = null;
	
	Rollback(Player player, Connection conn, String name, int minutes, String table) {
		this.player = player;
		this.conn = conn;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` WHERE player = ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDER BY date DESC", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, minutes);
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage(ChatColor.RED + "Error, check server logs.");
			return;
		}
	}
	
	Rollback(Player player, Connection conn, int radius, int minutes, String table) {
		this.player = player;
		this.conn = conn;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, data, replaced, x, y, z FROM `" + table + "` WHERE y > 0 and x > ? and x < ? and z > ? and z < ? AND date > date_sub(now(), INTERVAL ? MINUTE) ORDER BY date DESC", Statement.RETURN_GENERATED_KEYS);
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
	
	public void run()
	{
		ResultSet rs = null;
		edits.clear();
		try {
			rs = ps.executeQuery();
			while (rs.next()) {
				Edit e = new Edit(rs.getInt("type"), rs.getInt("replaced"), rs.getByte("data"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), player.getWorld());
				edits.offer(e);
			}
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
			player.sendMessage("§cError, check server logs.");
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
				player.sendMessage("§cError, check server logs.");
				return;
			}
		}
		int changes = edits.size();
		int rolledBack = 0;
		player.sendMessage(ChatColor.GREEN + "" + changes + " Changes found.");
		Edit e = edits.poll();
		while (e != null)
		{
			if (e.perform())
				rolledBack++;
			e = edits.poll();
		}
		player.sendMessage(ChatColor.GREEN + "Rollback finished successfully");
		player.sendMessage(ChatColor.GREEN + "Undid " + rolledBack + " of " + changes + " changes");
	}
	
	private class Edit
	{
		int type, replaced;
		int x, y, z;
		byte data;
		World world;
		
		Edit(int type, int replaced, byte data, int x, int y, int z, World world)
		{
			this.type = type;
			this.replaced = replaced;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.world = world;
		}
		
		public boolean perform()
		{
			Block block = world.getBlockAt(x, y, z);
			if (block.getTypeId() == type || (block.getTypeId() >= 8 && block.getTypeId() <= 11)) {
				if (block.setTypeId(replaced)) {
					block.setData(data);
					return true;	
				}
			}
			return false;
		}
	}
}