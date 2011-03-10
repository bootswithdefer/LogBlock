package com.bukkit.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AreaBlockSearch implements Runnable
{
	static final Logger log = Logger.getLogger("Minecraft");
	private Player player;
	private Location location;
	private int type;
	private int size;
	private Connection conn = null;
	
	AreaBlockSearch(Connection conn, Player player, int type, int size)
	{
		this.player = player;
		this.location = player.getLocation();
		this.type = type;
		this.size = size;
		this.conn = conn;
	}
	public void run()
	{
		boolean hist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Timestamp date;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd hh:mm:ss");
		
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * from blocks where (type = ? or replaced = ?) and y > ? and y < ? and x > ? and x < ? and z > ? and z < ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, type);
			ps.setInt(2, type);
			ps.setInt(3, location.getBlockY() - size);
			ps.setInt(4, location.getBlockY() + size);
			ps.setInt(5, location.getBlockX() - size);
			ps.setInt(6, location.getBlockX() + size);
			ps.setInt(7, location.getBlockZ() - size);
			ps.setInt(8, location.getBlockZ() + size);
			rs = ps.executeQuery();

			player.sendMessage("§3Block history within " + size + " blocks of  " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ": ");

			while (rs.next())
			{
				date = rs.getTimestamp("date");
				String datestr = formatter.format(date);
				String msg = datestr + " " + rs.getString("player") + " (" + rs.getInt("x") + ", " + rs.getInt("y") + ", " + rs.getInt("z") + ") ";
				if (rs.getInt("type") == 0)
					msg = msg + "destroyed " + Material.getMaterial(rs.getInt("replaced")).toString().toLowerCase().replace('_', ' ');
				else if (rs.getInt("replaced") == 0)
					msg = msg + "created " + Material.getMaterial(rs.getInt("type")).toString().toLowerCase().replace('_', ' ');
				else
					msg = msg + "replaced " + Material.getMaterial(rs.getInt("replaced")).toString().toLowerCase().replace('_', ' ') + " with " + Material.getMaterial(rs.getInt("type")).toString().toLowerCase().replace('_', ' ');
				player.sendMessage("§6" + msg);
				hist = true;
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
			}
		}
		if (!hist)
			player.sendMessage("§3None.");
	}
}
