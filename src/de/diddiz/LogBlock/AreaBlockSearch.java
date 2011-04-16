package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AreaBlockSearch implements Runnable
{
	private final Logger log;
	private final Player player;
	private final Location location;
	private final int type;
	private final int size;
	private final Connection conn;
	private final String table;

	AreaBlockSearch(LogBlock logblock, Player player, int type, int size) {
		this.player = player;
		this.location = player.getLocation();
		this.type = type;
		this.size = size;
		log = logblock.getServer().getLogger();
		conn = logblock.getConnection();
		table = logblock.getConfig().tables.get(player.getWorld().getName().hashCode());
	}

	public void run() {
		boolean hist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		try {
			if (conn == null) {
				player.sendMessage(ChatColor.RED + "Failed to create database connection");
				return;
			}
			if (table == null) {
				player.sendMessage(ChatColor.RED + "This world isn't logged");
				return;
			}
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * FROM `" + table + "` INNER JOIN `lb-players` USING (playerid) WHERE (type = ? or replaced = ?) and y > ? and y < ? and x > ? and x < ? and z > ? and z < ? order by date desc limit 10");
			ps.setInt(1, type);
			ps.setInt(2, type);
			ps.setInt(3, location.getBlockY() - size);
			ps.setInt(4, location.getBlockY() + size);
			ps.setInt(5, location.getBlockX() - size);
			ps.setInt(6, location.getBlockX() + size);
			ps.setInt(7, location.getBlockZ() - size);
			ps.setInt(8, location.getBlockZ() + size);
			rs = ps.executeQuery();
			player.sendMessage(ChatColor.DARK_AQUA + "Block history for " + getMaterialName(type) + " within " + size + " blocks of  " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ": ");
			while (rs.next()) {
				String msg = formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " (" + rs.getInt("x") + ", " + rs.getInt("y") + ", " + rs.getInt("z") + ") ";
				if (rs.getInt("type") == 0)
					msg = msg + "destroyed " + getMaterialName(rs.getInt("replaced"));
				else if (rs.getInt("replaced") == 0)
					msg = msg + "created " + getMaterialName(rs.getInt("type"));
				else
					msg = msg + "replaced " + getMaterialName(rs.getInt("replaced")) + " with " + getMaterialName(rs.getInt("type"));
				player.sendMessage(ChatColor.GOLD + msg);
				hist = true;
			}
			if (!hist)
				player.sendMessage(ChatColor.DARK_AQUA + "None.");
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock AreaBlockSearch] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock AreaBlockSearch] SQL exception on close", ex);
			}
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}
}
