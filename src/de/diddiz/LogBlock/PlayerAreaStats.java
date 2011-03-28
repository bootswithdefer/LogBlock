package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlayerAreaStats implements Runnable
{
	private Player player;
	private String name;
	private int size;
	private Connection conn = null;
	private String table;

	PlayerAreaStats(Connection conn, Player player, String name, int size, String table) {
		this.player = player;
		this.name = name;
		this.size = size;
		this.conn = conn;
		this.table = table;
	}

	public void run() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT `type`, SUM(`created`) AS `created`, SUM(`destroyed`) AS `destroyed` FROM ((SELECT `type`, count(`type`) AS `created`, 0 AS `destroyed` FROM `" + table + "` INNER JOIN `lb-players` USING (`playerid`) WHERE `playername` = ? AND x > ? AND x < ? AND z > ? AND z < ? AND `type` > 0 AND `type` != `replaced` GROUP BY `type`) UNION (SELECT `replaced` AS `type`, 0 AS `created`, count(`replaced`) AS `destroyed` FROM `" + table + "` INNER JOIN `lb-players` USING (`playerid`) WHERE `playername` = ? AND x > ? AND x < ? AND z > ? AND z < ? AND `replaced` > 0 AND `type` != `replaced` GROUP BY `replaced`)) AS t GROUP BY `type` ORDER BY SUM(`created`) + SUM(`destroyed`) DESC LIMIT 15");
			ps.setString(1, name);
			ps.setInt(2, player.getLocation().getBlockX()-size);
			ps.setInt(3, player.getLocation().getBlockX()+size);
			ps.setInt(4, player.getLocation().getBlockZ()-size);
			ps.setInt(5, player.getLocation().getBlockZ()+size);
			ps.setString(6, name);
			ps.setInt(7, player.getLocation().getBlockX()-size);
			ps.setInt(8, player.getLocation().getBlockX()+size);
			ps.setInt(9, player.getLocation().getBlockZ()-size);
			ps.setInt(10, player.getLocation().getBlockZ()+size);
			rs = ps.executeQuery();
			player.sendMessage(ChatColor.DARK_AQUA + "Player " + name + " within " + size + " blocks of you: ");
			if (!rs.next())
				player.sendMessage(ChatColor.DARK_AQUA + "No results found.");
			else {
				player.sendMessage(ChatColor.GOLD + String.format("%-6s %-6s %s", "Creat", "Destr", "Block"));
				rs.beforeFirst();
				while (rs.next()) {
					player.sendMessage(ChatColor.GOLD + String.format("%-6d %-6d %s", rs.getInt("created"), rs.getInt("destroyed"), Material.getMaterial(rs.getInt("type")).toString().toLowerCase().replace('_', ' ')));
				}
			}
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, "[LogBlock PlayerAreaStats] SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock PlayerAreaStats] SQL exception on close", ex);
			}
		}
	}
}
