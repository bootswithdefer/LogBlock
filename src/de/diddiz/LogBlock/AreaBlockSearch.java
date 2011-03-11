package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AreaBlockSearch implements Runnable
{
	private Player player;
	private Location location;
	private int type;
	private int size;
	private Connection conn = null;
	private String table;

	AreaBlockSearch(Connection conn, Player player, int type, int size, String table) {
		this.player = player;
		this.location = player.getLocation();
		this.type = type;
		this.size = size;
		this.conn = conn;
		this.table = table;
	}

	public void run() {
		boolean hist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * FROM `" + table + "` INNER JOIN `players` USING (`playerid`) WHERE (type = ? or replaced = ?) and y > ? and y < ? and x > ? and x < ? and z > ? and z < ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
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
			LogBlock.log.log(Level.SEVERE, "[LogBlock AreaBlockSearch] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock AreaBlockSearch] SQL exception on close", ex);
			}
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}
}
