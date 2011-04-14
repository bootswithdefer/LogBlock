package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;

public class Consumer extends TimerTask implements Runnable
{
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	private LinkedBlockingQueue<KillRow> kqueue = new LinkedBlockingQueue<KillRow>();
	private HashSet<Integer> hiddenplayers = new HashSet<Integer>();
	private HashMap<Integer, Integer> lastAttackedEntity = new HashMap<Integer, Integer>();
	private HashMap<Integer, Long> lastAttackTime = new HashMap<Integer, Long>();
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
			row.signtext = signtext.replace("\\", "\\\\").replace("'", "\\'");
		if (ca != null)
			row.ca = ca;
		if (!bqueue.offer(row))
			LogBlock.log.info("[LogBlock] Failed to queue block for " + playerName);
	}

	public void queueKill(Entity attacker, Entity defender) {
		if (lastAttackedEntity.containsKey(attacker.getEntityId()) && lastAttackedEntity.get(attacker.getEntityId()) == defender.getEntityId() && System.currentTimeMillis() - lastAttackTime.get(attacker.getEntityId()) < 3000)
			return;
		String table = LogBlock.config.tables.get(defender.getWorld().getName().hashCode());
		if (table == null)
			return;
		int weapon = 0;
		if (attacker instanceof Player && ((Player)attacker).getItemInHand() != null)
			weapon = ((Player)attacker).getItemInHand().getTypeId();
		String attackerName = getEntityName(attacker);
		String defenderName = getEntityName(defender);
		if (attackerName == null || defenderName == null)
			return;
		lastAttackedEntity.put(attacker.getEntityId(), defender.getEntityId());
		lastAttackTime.put(attacker.getEntityId(), System.currentTimeMillis());
		kqueue.add(new KillRow(table, getEntityName(attacker), getEntityName(defender), weapon));
	}

	public int getQueueSize() {
		return bqueue.size() + kqueue.size();
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
		BlockRow b; KillRow k;
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
			while (!kqueue.isEmpty() && count < 1000 && (System.currentTimeMillis() - start < 100 || count < 100)) {
				k = kqueue.poll();
				if (k == null)
					continue;
				state.execute("INSERT INTO `" + k.table + "-kills` (date, killer, victim, weapon) SELECT now(), playerid, (SELECT playerid FROM `lb-players` WHERE playername = '" + k.victim + "'), " + k.weapon + " FROM `lb-players` WHERE playername = '" + k.killer + "'");
			}
			conn.commit();
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception", ex);
		} finally {
			try {
				if (conn != null)
					conn.close();
				if (state != null)
					state.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock Consumer] SQL exception on close", ex);
			}
		}
	}

	private String getEntityName(Entity entity) {
		if (entity instanceof Player)
			return ((Player)entity).getName();
		if (entity instanceof Creeper)
			return "Creeper";
		if (entity instanceof Ghast)
			return "Ghast";
		if (entity instanceof Giant)
			return "Giant";
		if (entity instanceof PigZombie)
			return "PigZombie";
		if (entity instanceof Skeleton)
			return "Skeleton";
		if (entity instanceof Slime)
			return "Slime";
		if (entity instanceof Spider)
			return "Spider";
		if (entity instanceof Wolf)
			return "Wolf";
		if (entity instanceof Zombie)
			return "Zombie";
		return null;
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

	private class KillRow
	{
		public String table;
		public String killer;
		public String victim;
		public int weapon;

		KillRow(String table, String attacker, String defender, int weapon) {
			this.table = table;
			this.killer = attacker;
			this.victim = defender;
			this.weapon = weapon;
		}

		@Override
		public int hashCode() {
			return killer.hashCode() * 31 + victim.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			KillRow k = (KillRow)obj;
			if (!killer.equals(k.killer))
				return false;
			if (!victim.equals(k.victim))
				return false;
			return true;
		}
	}
}
