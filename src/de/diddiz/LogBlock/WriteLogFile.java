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
				int type = rs.getInt("type");
				int replaced = rs.getInt("replaced");
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