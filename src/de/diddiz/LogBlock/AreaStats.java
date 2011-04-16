package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class AreaStats implements Runnable
{
	private final Logger log;
	private final Player player;
	private final int size;
	private final Connection conn;
	private final String table;

	AreaStats(LogBlock logblock, Player player, int size) {
		this.player = player;
		this.size = size;
		log = logblock.getServer().getLogger();
		conn = logblock.getConnection();
		table = logblock.getConfig().tables.get(player.getWorld().getName().hashCode());
	}

	public void run() {
		PreparedStatement ps = null;
		ResultSet rs = null;
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
			ps = conn.prepareStatement("SELECT `playername`, SUM(`created`) AS `created`, SUM(`destroyed`) AS `destroyed` FROM ((SELECT `playerid`, count(`type`) AS `created`, 0 AS `destroyed` FROM `" + table + "` WHERE `type` > 0 AND x > ? AND x < ? AND z > ? AND z < ?	AND `type` != `replaced` GROUP BY `playerid`) UNION (SELECT `playerid`, 0 AS `created`, count(`replaced`) AS `destroyed` FROM `" + table + "` WHERE `replaced` > 0 AND x > ? AND x < ? AND z > ? AND z < ? AND `type` != `replaced` GROUP BY `playerid`)) AS t INNER JOIN `lb-players` USING (`playerid`) GROUP BY `playerid` ORDER BY SUM(`created`) + SUM(`destroyed`) DESC LIMIT 15");
			ps.setInt(1, player.getLocation().getBlockX()-size);
			ps.setInt(2, player.getLocation().getBlockX()+size);
			ps.setInt(3, player.getLocation().getBlockZ()-size);
			ps.setInt(4, player.getLocation().getBlockZ()+size);
			ps.setInt(5, player.getLocation().getBlockX()-size);
			ps.setInt(6, player.getLocation().getBlockX()+size);
			ps.setInt(7, player.getLocation().getBlockZ()-size);
			ps.setInt(8, player.getLocation().getBlockZ()+size);
			rs = ps.executeQuery();
			player.sendMessage(ChatColor.DARK_AQUA + "Within " + size + " blocks of you: ");
			if (!rs.next())
				player.sendMessage(ChatColor.DARK_AQUA + "No results found.");
			else {
				player.sendMessage(ChatColor.GOLD + String.format("%-6s %-6s %s", "Creat", "Destr", "Player"));
				rs.beforeFirst();
				while (rs.next()) {
					player.sendMessage(ChatColor.GOLD + String.format("%-6d %-6d %s", rs.getInt("created"), rs.getInt("destroyed"), rs.getString("playername")));
				}
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock AreaStats] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock AreaStats] SQL exception on close", ex);
			}
		}
	}
}
