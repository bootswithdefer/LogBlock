package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockStats implements Runnable
{
	private final Logger log;
	private final Connection conn;
	private final String table;
	private final Player player;
	private final Block block;

	BlockStats(LogBlock logblock, Player player, Block block) {
		log = logblock.getServer().getLogger();
		conn = logblock.getConnection();
		table = logblock.getConfig().tables.get(player.getWorld().getName().hashCode());
		this.player = player;
		this.block = block;
	}

	@Override
	public void run() {
		boolean hist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		final SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
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
			ps = conn.prepareStatement("SELECT date, replaced, type, signtext, playername FROM `" + table + "` LEFT JOIN `" + table + "-sign` USING (id) INNER JOIN `lb-players` USING (playerid) WHERE x = ? AND y = ? AND z = ? ORDER BY date DESC LIMIT 15");
			ps.setInt(1, block.getX());
			ps.setInt(2, block.getY());
			ps.setInt(3, block.getZ());
			rs = ps.executeQuery();
			player.sendMessage(ChatColor.DARK_AQUA + "Block history (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + "): ");
			while (rs.next()) {
				String msg = formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " ";
				final int type = rs.getInt("type");
				final int replaced = rs.getInt("replaced");
				if ((type == 63 || type == 68) && rs.getString("signtext") != null)
					msg += "created " + rs.getString("signtext");
				else if (type == replaced) {
					if (type == 23 || type == 54 || type == 61)
						msg += "looked inside " + getMaterialName(type);
				} else if (type == 0)
					msg += "destroyed " + getMaterialName(replaced);
				else if (replaced == 0)
					msg += "created " + getMaterialName(type);
				else
					msg += "replaced " + getMaterialName(replaced) + " with " + getMaterialName(type);
				player.sendMessage(ChatColor.GOLD + msg);
				hist = true;
			}
			if (!hist)
				player.sendMessage(ChatColor.DARK_AQUA + "None.");
		} catch (final SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock BlockStats] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (final SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock BlockStats] SQL exception on close", ex);
			}
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}
}