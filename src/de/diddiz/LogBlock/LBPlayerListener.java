package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;

public class LBPlayerListener extends PlayerListener
{
	private final Logger log;
	private final LogBlock logblock;
	private final Consumer consumer;

	LBPlayerListener(LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
		consumer = logblock.getConsumer();
	}

	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled()) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (event.getClickedBlock().getType() == Material.CHEST || event.getClickedBlock().getType() == Material.FURNACE ||event.getClickedBlock().getType() == Material.DISPENSER)) {
				consumer.queueBlock(event.getPlayer(), event.getClickedBlock(), (short)0, (byte)0, (short)0, (byte)0);
			}
		}
	}	

	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (!event.isCancelled()) {
			consumer.queueBlock(event.getPlayer().getName(), event.getBlockClicked(), event.getBlockClicked().getTypeId(), 0, event.getBlockClicked().getData());
		}
	}

	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (event.getBucket() == Material.WATER_BUCKET)
			consumer.queueBlock(event.getPlayer(), event.getBlockClicked().getFace(event.getBlockFace()), Material.STATIONARY_WATER.getId());
		else if (event.getBucket() == Material.LAVA_BUCKET)
			consumer.queueBlock(event.getPlayer(), event.getBlockClicked().getFace(event.getBlockFace()), Material.STATIONARY_LAVA.getId());
	}

	public void onPlayerJoin(PlayerJoinEvent event) {
		Connection conn = logblock.getConnection();
		if (conn == null)
			return;
		Statement state = null;
		try {
			state = conn.createStatement();
			state.execute("INSERT IGNORE INTO `lb-players` (playername) VALUES ('" + event.getPlayer().getName() + "');");
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
	}
}
