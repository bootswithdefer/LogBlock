package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
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
		HashSet<String> types = new HashSet<String>();
		HashMap<String, Integer> created = new HashMap<String, Integer>();
		HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, count(type) as num from `" + table + "` INNER JOIN `players` USING (`playerid`) where type > 0 and playername = ? and y > 0 and x > ? and x < ? and z > ? and z < ? group by type order by count(replaced) desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, player.getLocation().getBlockX()-size);
			ps.setInt(3, player.getLocation().getBlockX()+size);
			ps.setInt(4, player.getLocation().getBlockZ()-size);
			ps.setInt(5, player.getLocation().getBlockZ()+size);
			rs = ps.executeQuery();
			while (rs.next()) {
				types.add(getMaterialName(rs.getInt("type")));
				created.put(getMaterialName(rs.getInt("type")), rs.getInt("num"));
			}
			rs.close();
			ps.close();
			ps = conn.prepareStatement("SELECT replaced, count(replaced) as num from `" + table + "` INNER JOIN `players` USING (`playerid`) where replaced > 0 and playername = ? and y > 0 and x > ? and x < ? and z > ? and z < ? group by replaced order by count(replaced) desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, player.getLocation().getBlockX()-size);
			ps.setInt(3, player.getLocation().getBlockX()+size);
			ps.setInt(4, player.getLocation().getBlockZ()-size);
			ps.setInt(5, player.getLocation().getBlockZ()+size);
			rs = ps.executeQuery();
			while (rs.next()) {
				types.add(getMaterialName(rs.getInt("replaced")));
				destroyed.put(getMaterialName(rs.getInt("replaced")), rs.getInt("num"));
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
		player.sendMessage(ChatColor.DARK_AQUA + "Player " + name + " within " + size + " blocks of you: ");
		if (types.size() == 0)
			player.sendMessage(ChatColor.DARK_AQUA + "No results found.");
		else {
			player.sendMessage(ChatColor.GOLD + String.format("%-6s %-6s %s", "Creat", "Destr", "Block"));
			for (String t: types) {
				Integer c = created.get(t);
				Integer d = destroyed.get(t);
				if (c == null)
					c = 0;
				if (d == null)
					d = 0;
				player.sendMessage(ChatColor.GOLD + String.format("%-6d %-6d %s", c, d, t));
			}
		}
	}

	private String getMaterialName(int type) {
		return Material.getMaterial(type).toString().toLowerCase().replace('_', ' ');
	}
}
