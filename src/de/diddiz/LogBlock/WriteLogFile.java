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
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WriteLogFile implements Runnable
{
	private final Logger log;
	private final Connection conn;
	private final Player player;
	private final String name;
	private final String table;

	WriteLogFile(LogBlock logblock, Player player, String name) {
		this.player = player;
		this.name = name;
		log = logblock.getServer().getLogger();
		conn = logblock.getConnection();
		table = logblock.getConfig().tables.get(player.getWorld().getName().hashCode());
	}

	@Override
	public void run() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		final SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
		final String newline = System.getProperty("line.separator");
		String msg;
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
			ps = conn.prepareStatement("SELECT * FROM `" + table + "` LEFT JOIN `" + table + "-sign` USING (id) INNER JOIN `lb-players` USING (playerid) WHERE playername = ? ORDER BY date ASC");
			ps.setString(1, name);
			rs = ps.executeQuery();
			final File file = new File ("plugins/LogBlock/log/" + name + ".log");
			file.getParentFile().mkdirs();
			final FileWriter writer = new FileWriter(file);
			player.sendMessage(ChatColor.GREEN + "Creating " + file.getName());
			while (rs.next()) {
				msg = formatter.format(rs.getTimestamp("date")) + " " + rs.getString("playername") + " ";
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
				writer.write(msg + newline);
			}
			writer.close();
			player.sendMessage(ChatColor.GREEN + "Done");
		} catch (final SQLException ex) {
			player.sendMessage(ChatColor.RED + "SQL exception");
			log.log(Level.SEVERE, "[LogBlock WriteLogFile] SQL exception", ex);
		} catch (final IOException ex) {
			player.sendMessage(ChatColor.RED + "IO exception");
			log.log(Level.SEVERE, "[LogBlock WriteLogFile] IO exception", ex);
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