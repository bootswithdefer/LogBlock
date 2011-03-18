package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockStats implements Runnable
{
	private Player player;
	private Block block;
	private Connection conn;
	private String table;

	BlockStats(Connection conn, Player player, Block block, String table) {
		this.player = player;
		this.conn = conn;
		this.block = block;
		this.table = table;
	}

	@Override
	public void run() {
		if (conn == null) {
			player.sendMessage(ChatColor.RED + "Failed to create database connection");
			return;
		}
		if (table == null) {
			player.sendMessage(ChatColor.RED + "This world isn't logged");
			return;
		}
		boolean hist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * FROM `" + table + "` LEFT JOIN `" + table + "-sign` USING (`id`) INNER JOIN `lb-players` USING (`playerid`) WHERE `x` = ? AND `y` = ? AND `z` = ? ORDER BY `date` DESC LIMIT 15", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, block.getX());
			ps.setInt(2, block.getY());
			ps.setInt(3, block.getZ());
			rs = ps.executeQuery();
			player.sendMessage(ChatColor.DARK_AQUA + "Block history (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + "): ");
			while (rs.next()) {
				String msg = formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " ";
				if ((rs.getInt("type") == 63 || rs.getInt("type") == 68) && rs.getString("signtext") != null)
					msg += "created " + rs.getString("signtext");
				else if (rs.getInt("type") == 54 && rs.getInt("replaced") == 54)
					msg += "looked inside";
				else if (rs.getInt("type") == 0)
					msg += "destroyed " + getMaterialName(rs.getInt("replaced"));
				else if (rs.getInt("replaced") == 0)
					msg += "created " + getMaterialName(rs.getInt("type"));
				else
					msg += "replaced " + getMaterialName(rs.getInt("replaced")) + " with " + getMaterialName(rs.getInt("type"));
				player.sendMessage(ChatColor.GOLD + msg);
				hist = true;
			}
			if (!hist)
				player.sendMessage(ChatColor.DARK_AQUA + "None.");
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, "[LogBlock BlockStats] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock BlockStats] SQL exception on close", ex);
			}
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}
}