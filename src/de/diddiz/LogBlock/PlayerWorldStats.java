package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

import org.bukkit.entity.Player;

public class PlayerWorldStats implements Runnable
{
	private Player player;
	private Connection conn = null;
	private String table;

	PlayerWorldStats(Connection conn, Player player, String table)
	{
		this.player = player;
		this.conn = conn;
		this.table = table;
	}
	public void run()
	{
		HashSet<String> players = new HashSet<String>();
		HashMap<String, Integer> created = new HashMap<String, Integer>();
		HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT player, count(player) as num from `" + table + "` where type > 0 group by player order by count(player) desc limit 5", Statement.RETURN_GENERATED_KEYS);
			rs = ps.executeQuery();
			while (rs.next())
			{
				players.add(rs.getString("player"));
				created.put(rs.getString("player"), rs.getInt("num"));
			}
			rs.close();
			ps.close();
			
			ps = conn.prepareStatement("SELECT player, count(player) as num from `" + table + "` where replaced > 0 group by player order by count(player) desc limit 5", Statement.RETURN_GENERATED_KEYS);
			rs = ps.executeQuery();
			while (rs.next())
			{
				players.add(rs.getString("player"));
				destroyed.put(rs.getString("player"), rs.getInt("num"));
			}
			
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
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
			}
		}

		player.sendMessage("§3Within entire world:");
		if (players.size() == 0)
		{
			player.sendMessage("§3No results found.");
			return;
		}
		
		player.sendMessage("§6" + String.format("%-6s %-6s %s", "Creat", "Destr", "Player"));
		for (String p: players)
		{
			Integer c = created.get(p);
			Integer d = destroyed.get(p);
			if (c == null)
				c = 0;
			if (d == null)
				d = 0;
			player.sendMessage("§6" + String.format("%-6d %-6d %s", c, d, p));
		}
	}
}
