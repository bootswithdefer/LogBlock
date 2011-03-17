package de.diddiz.LogBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WriteLogFile implements Runnable
{
	private Connection conn;
	private Player player;
	private String name;
	private String table;

	WriteLogFile(Connection conn, Player player, String name, String table) {
		this.conn = conn;
		this.player = player;
		this.name = name;
		this.table = table;
	}

	@Override
	public void run() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		String newline = System.getProperty("line.separator");
		String msg;
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * FROM `" + table + "` LEFT JOIN `" + table + "-sign` USING (`id`) INNER JOIN `lb-players` USING (`playerid`) WHERE `playername` = ? ORDER BY `date` ASC");
			ps.setString(1, name);
			rs = ps.executeQuery();
			File file = new File ("plugins/LogBlock/log/" + name + ".log");
			file.getParentFile().mkdirs();
			FileWriter writer = new FileWriter(file);
			player.sendMessage(ChatColor.GREEN + "Creating " + file.getName());
			while (rs.next()) {
				msg = formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " ";
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
				writer.write(msg + newline);
			}
			writer.close();
			player.sendMessage(ChatColor.GREEN + "Done");
		} catch (SQLException ex) {
			player.sendMessage(ChatColor.RED + "SQL exception");
			LogBlock.log.log(Level.SEVERE, "[LogBlock WriteLogFile] SQL exception", ex);
		} catch (IOException ex) {
			player.sendMessage(ChatColor.RED + "IO exception");
			LogBlock.log.log(Level.SEVERE, "[LogBlock WriteLogFile] IO exception", ex);
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