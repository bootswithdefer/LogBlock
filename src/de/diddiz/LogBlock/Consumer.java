package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Consumer implements Runnable
{
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	private HashSet<Integer> hiddenplayers = new HashSet<Integer>();
	private LogBlock logblock;

	Consumer (LogBlock logblock) {
		this.logblock = logblock;
	}

	public void queueBlock(Player player, Block block, int typeAfter) {
		queueBlock(player.getName(), block, 0, typeAfter, (byte)0, null, null);
	}

	public void queueBlock(String playerName, Block block, int typeBefore, int typeAfter, byte data) {
		queueBlock(playerName, block, typeBefore, typeAfter, data, null, null);
	}

	public void queueBlock(Player player, Block block, short inType, byte inAmount, short outType, byte outAmount) {
		queueBlock(player.getName(), block, block.getTypeId(), block.getTypeId(), (byte)0, null, new ChestAccess(inType, inAmount, outType, outAmount));
	}

	public void queueBlock(String playerName, Block block, int typeBefore, int typeAfter, byte data, String signtext, ChestAccess ca) {
		if (block == null || typeBefore < 0 || typeAfter < 0)
			return;
		if (hiddenplayers.contains(playerName.hashCode()))
			return;
		String table = LogBlock.config.tables.get(block.getWorld().getName().hashCode());
		if (table == null)
			return;
		if (playerName.length() > 32)
			playerName = playerName.substring(0, 32);
		BlockRow row = new BlockRow(table, playerName, typeBefore, typeAfter, data, block.getX(), block.getY(), block.getZ());
		if (signtext != null)
			row.signtext = signtext.replace("'", "\\'");
		if (ca != null)
			row.ca = ca;
		if (!bqueue.offer(row))
			LogBlock.log.info("[LogBlock] Failed to queue block for " + playerName);
	}

	public int getQueueSize() {
		return bqueue.size();
	}

	public boolean hide(Player player) {
		int hash = player.getName().hashCode();
		if (hiddenplayers.contains(hash)) {
			hiddenplayers.remove(hash);
			return false;
		} else {
			hiddenplayers.add(hash);
			return true;
		}
	}
	
	public synchronized void run() {
		Connection conn = logblock.pool.getConnection();
		if (conn == null)
			return;
		Statement state = null;
		BlockRow b;
		int count = 0;
		if (bqueue.size() > 100)
			LogBlock.log.info("[LogBlock Consumer] Queue overloaded. Size: " + bqueue.size());				
		try {
			conn.setAutoCommit(false);
			state = conn.createStatement();
			long start = System.currentTimeMillis();
			while (count < 1000 && !bqueue.isEmpty() && (System.currentTimeMillis() - start < 100 || count < 100)) {
				b = bqueue.poll();
				if (b == null)
					continue;
				state.execute("INSERT INTO `" + b.table + "` (date, playerid, replaced, type, data, x, y, z) SELECT now(), playerid, " + b.replaced + ", " + b.type + ", " + b.data + ", '" + b.x + "', " + b.y + ", '" + b.z + "' FROM `lb-players` WHERE playername = '" + b.name + "'", Statement.RETURN_GENERATED_KEYS);
				if (b.signtext != null) {
					ResultSet keys = state.getGeneratedKeys();
					if (keys.next())
						state.execute("INSERT INTO `" + b.table + "-sign` (id, signtext) values (" + keys.getInt(1) + ", '" + b.signtext + "')");
					else
						LogBlock.log.severe("[LogBlock Consumer] Failed to get generated keys");
				} else if (b.ca != null) {
					ResultSet keys = state.getGeneratedKeys();
					if (keys.next())
						state.execute("INSERT INTO `" + b.table + "-chest` (id, intype, inamount, outtype, outamount) values (" + keys.getInt(1) + ", " + b.ca.inType + ", " + b.ca.inAmount + ", " + b.ca.outType + ", " + b.ca.outAmount + ")");
					else
						LogBlock.log.severe("[LogBlock Consumer] Failed to get generated keys");
				}
				count++;
			}
			conn.commit();
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception on close", ex);
			}
		}
	}

	private class ChestAccess
	{
		public short inType, outType;
		public byte inAmount, outAmount;

		ChestAccess(short inType, byte inAmount, short outType, byte outAmount) {
			this.inType = inType;
			this.inAmount = inAmount;
			this.outType = outType;
			this.outAmount = outAmount;
		}
	}

	private class BlockRow
	{
		public String table;
		public String name;
		public int replaced, type;
		public byte data;
		public int x, y, z;
		public String signtext;
		public ChestAccess ca;

		BlockRow(String table, String name, int replaced, int type, byte data, int x, int y, int z)	{
			this.table = table;
			this.name = name;
			this.replaced = replaced;
			this.type = type;
			this.data = data;
			this.x = x;
			this.y = y;
			this.z = z;
			this.signtext = null;
			this.ca = null;
		}
	}
}
